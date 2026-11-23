package com.smartmall.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.cart.service.CartService;
import com.smartmall.catalog.domain.Product;
import com.smartmall.catalog.domain.Sku;
import com.smartmall.catalog.service.ProductService;
import com.smartmall.catalog.service.SkuStockService;
import com.smartmall.common.api.PageResult;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.config.MallProperties;
import com.smartmall.common.exception.BizException;
import com.smartmall.order.domain.CartItem;
import com.smartmall.order.domain.Order;
import com.smartmall.order.domain.OrderItem;
import com.smartmall.order.domain.OrderLog;
import com.smartmall.order.dto.CreateOrderRequest;
import com.smartmall.order.enums.OperatorType;
import com.smartmall.order.enums.OrderStatus;
import com.smartmall.order.mapper.OrderItemMapper;
import com.smartmall.order.mapper.OrderLogMapper;
import com.smartmall.order.mapper.OrderMapper;
import com.smartmall.order.service.OrderService;
import com.smartmall.order.vo.OrderVO;
import com.smartmall.pay.enums.PayChannel;
import com.smartmall.user.service.AddressService;
import com.smartmall.user.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务实现。
 *
 * <p>下单这段代码只有一个关键顺序：<b>先扣库存，再写订单</b>，且全部在同一个事务里。
 * 反过来（先写订单再扣库存）一旦扣减失败，就得靠回滚删订单，
 * 而订单号已经发给前端了，回滚后前端拿着一张不存在的单。
 *
 * <p>扣库存用的 {@code SkuStockService.deduct} 是<b>条件更新</b>：
 * {@code update pms_sku set stock = stock - n where stock >= n}，
 * 返回 0 行就代表没扣成。所以这里不需要任何锁，也不存在"查完再判"的竞态。
 *
 * <p>跨包纪律：本类只注入 {@code CartService}、{@code SkuStockService}、
 * {@code ProductService}、{@code AddressService} 四个接口，
 * <b>没有注入任何别人的 Mapper</b>。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 订单号里的时间部分：精确到秒 */
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 订单号后 4 位随机，降低同秒并发撞号概率（真实业务该用发号器） */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * JSON 工具：收货快照序列化。
     *
     * <p>不注入 Spring 的 ObjectMapper：本项目只用它读写「字符串 Map」，
     * 自带一个实例即可，也就不依赖上下文里是否注册了该 Bean。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 订单主表 */
    private final OrderMapper orderMapper;

    /** 订单明细 */
    private final OrderItemMapper orderItemMapper;

    /** 状态流水 */
    private final OrderLogMapper orderLogMapper;

    /** 购物车：读勾选项、下单后清理 */
    private final CartService cartService;

    /** 库存：扣减与回补 */
    private final SkuStockService skuStockService;

    /** 商品：快照要标题与主图 */
    private final ProductService productService;

    /** 地址：生成收货快照 */
    private final AddressService addressService;

    /** 业务配置：运费、包邮门槛、支付超时 */
    private final MallProperties properties;

    /**
     * 下单。
     *
     * <p>四步：幂等检查 → 读购物车 → 扣库存并组装快照 → 落库并清车。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long userId, CreateOrderRequest request) {
        // 1. 幂等：同一个 requestNo 已经下过单，直接返回那张单，不重复扣库存
        Order exist = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getRequestNo, request.requestNo()));
        if (exist != null) {
            // 别人的 requestNo 不能拿来查单，统一按"订单不存在"处理
            if (!exist.ownedBy(userId)) {
                throw new BizException(ResultCode.ORDER_NOT_FOUND);
            }
            log.debug("重复下单请求命中幂等键 requestNo={} orderNo={}", request.requestNo(), exist.getOrderNo());
            return detail(userId, exist.getOrderNo());
        }

        // 2. 读勾选的购物车条目：没勾任何东西就没什么可下的
        List<CartItem> cartItems = cartService.listChecked(userId);
        if (cartItems.isEmpty()) {
            throw new BizException(ResultCode.CART_EMPTY);
        }

        // 3. 地址双条件校验：拿不到说明不存在或不是你的，40400
        AddressVO address = addressService.get(userId, request.addressId());

        // 商品信息按 SPU 缓存，避免同一商品多个 SKU 时重复查库
        Map<Long, Product> productCache = new HashMap<>();

        // 明细先攒在内存里，等订单主表拿到 ID 再统一落库
        List<OrderItem> items = new ArrayList<>(cartItems.size());

        // 商品总额，逐行累加
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            // 3.1 扣库存：条件更新，返回的是"扣减前"的规格快照（价格、规格文案都在里面）
            //     库存不足会抛 12003，异常直接让整个事务回滚，购物车条目不受影响
            Sku sku = skuStockService.deduct(cartItem.getSkuId(), cartItem.getQuantity());

            // 3.2 商品标题与主图：同一 SPU 只查一次
            Product product = productCache.computeIfAbsent(cartItem.getProductId(), productService::getProduct);

            // 3.3 快照价格取 SKU 的价，且此刻之后不再回查商品表
            BigDecimal price = sku.getPrice() == null ? BigDecimal.ZERO : sku.getPrice();

            // 3.4 行小计 = 单价 × 数量
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            // 3.5 组装明细快照
            OrderItem item = new OrderItem();
            item.setProductId(sku.getProductId());
            item.setSkuId(sku.getId());
            item.setProductTitle(product.getTitle());
            item.setSkuSpec(sku.getSpec());
            item.setProductImage(product.getMainImage());
            item.setPrice(price);
            item.setQuantity(cartItem.getQuantity());
            item.setTotalAmount(lineTotal);
            items.add(item);

            // 3.6 累加商品总额
            totalAmount = totalAmount.add(lineTotal);
        }

        // 4. 运费：满门槛包邮，否则按配置收固定运费
        BigDecimal threshold = properties.order().freeFreightThreshold();
        BigDecimal freight = totalAmount.compareTo(threshold) >= 0
                ? BigDecimal.ZERO
                : properties.order().freight();

        // 5. 优惠当前未接入，先记 0，字段留着不删
        BigDecimal discount = BigDecimal.ZERO;

        // 6. 实付 = 商品总额 - 优惠 + 运费，支付单直接取这个值，不再二次计算
        BigDecimal payAmount = totalAmount.subtract(discount).add(freight).setScale(2, RoundingMode.HALF_UP);

        // 7. 写订单主表
        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountAmount(discount);
        order.setCouponAmount(BigDecimal.ZERO);
        order.setFreightAmount(freight);
        order.setPayAmount(payAmount);
        order.setStatus(OrderStatus.WAIT_PAY);                                  // 下单即待支付
        order.setExpireTime(LocalDateTime.now().plusMinutes(properties.order().payTimeoutMinutes()));
        order.setReceiverSnapshot(toSnapshot(address));                         // 收货信息定格在这一刻
        order.setRequestNo(request.requestNo());
        order.setItemCount(items.stream().mapToInt(OrderItem::getQuantity).sum());
        orderMapper.insert(order);                                              // 雪花 ID 由 MP 回填

        // 8. 明细落库：依赖上一步回填的 ID
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            orderItemMapper.insert(item);
        }

        // 9. 记状态流水：from 为 null 表示订单诞生
        writeLog(order, null, OrderStatus.WAIT_PAY, OperatorType.USER, userId, "用户下单");

        // 10. 清掉已结算的购物车条目：只删本次下单的那些 ID
        cartService.removeByIds(userId, cartItems.stream().map(CartItem::getId).toList());

        log.info("下单成功 orderNo={} userId={} 件数={} 实付={}",
                order.getOrderNo(), userId, order.getItemCount(), payAmount);

        return OrderVO.of(order, items, summaryOf(order.getReceiverSnapshot()));
    }

    @Override
    public PageResult<OrderVO> page(Long userId, String status, long pageNum, long pageSize) {
        // 页码与条数兜底：前端传 0 或超大值都不该让 SQL 变形
        long current = Math.max(1, pageNum);
        long size = Math.min(Math.max(1, pageSize), 50);

        // 状态过滤可选；传了非法值直接 40000，而不是悄悄当"全部"
        OrderStatus st = parseStatus(status);

        var wrapper = Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)                        // 只能看自己的订单
                .orderByDesc(Order::getId);                          // 新单在前

        if (st != null) {
            wrapper.eq(Order::getStatus, st);
        }

        Page<Order> page = orderMapper.selectPage(new Page<>(current, size), wrapper);

        // 列表页需要缩略图与商品名，这里一次性把本页所有订单的明细捞出来再按订单分组：
        // 一页 5 条也就 1 次 IN 查询，比「每条订单查一次」的 N+1 好得多。
        List<Order> orders = page.getRecords();
        Map<Long, List<OrderItem>> itemsByOrder = new HashMap<>();
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).toList();
            orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                            .in(OrderItem::getOrderId, orderIds)
                            .orderByAsc(OrderItem::getId))
                    .forEach(item -> itemsByOrder
                            .computeIfAbsent(item.getOrderId(), k -> new ArrayList<>())
                            .add(item));
        }

        return PageResult.of(page, o -> OrderVO.of(o,
                itemsByOrder.getOrDefault(o.getId(), List.of()),
                summaryOf(o.getReceiverSnapshot())));
    }

    @Override
    public OrderVO detail(Long userId, String orderNo) {
        Order order = getOwned(userId, orderNo);
        List<OrderItem> items = listItems(order.getId());
        return OrderVO.of(order, items, summaryOf(order.getReceiverSnapshot()));
    }

    /**
     * 取消订单：只有待支付能取消。
     *
     * <p>顺序很关键：<b>先条件更新改状态，成功了再回补库存</b>。
     * 反过来的话，两个并发取消请求会各回补一次库存，凭空多出一倍货。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, String orderNo) {
        Order order = getOwned(userId, orderNo);

        // 条件更新：where status = WAIT_PAY，改不到就是状态不允许（或已被并发取消）
        int rows = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.WAIT_PAY)
                .set(Order::getStatus, OrderStatus.CANCELLED)
                .set(Order::getCloseTime, LocalDateTime.now()));

        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL, "只有待支付的订单可以取消");
        }

        // 状态已经安全落地，再逐件回补库存
        for (OrderItem item : listItems(order.getId())) {
            skuStockService.restore(item.getSkuId(), item.getQuantity());
        }

        writeLog(order, OrderStatus.WAIT_PAY, OrderStatus.CANCELLED, OperatorType.USER, userId, "用户取消订单");
        log.info("订单已取消 orderNo={} userId={}", orderNo, userId);
    }

    @Override
    public Order requireOrder(String orderNo, Long userId) {
        return getOwned(userId, orderNo);
    }

    /**
     * 支付成功推进状态：待支付 → 待发货。
     *
     * <p>幂等靠条件更新实现：{@code where status = WAIT_PAY}，
     * 第三方重复回调时第二次影响 0 行，此时看是不是已经是待发货——是就直接返回。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(String orderNo, PayChannel channel) {
        Order order = getByOrderNo(orderNo);

        int rows = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.WAIT_PAY)
                .set(Order::getStatus, OrderStatus.WAIT_SHIP)
                .set(Order::getPayType, channel)
                .set(Order::getPayTime, LocalDateTime.now()));

        if (rows == 0) {
            // 没改到：要么已经处理过（幂等，直接返回），要么订单已被关单
            Order latest = getByOrderNo(orderNo);
            if (latest.getStatus() == OrderStatus.WAIT_SHIP) {
                log.debug("订单已处理过支付回调，幂等返回 orderNo={}", orderNo);
                return;
            }
            // 钱付了单却关了：真实业务要在这里发起退款
            throw new BizException(ResultCode.ORDER_PAY_TIMEOUT, "订单已关闭，支付款将原路退回");
        }

        writeLog(order, OrderStatus.WAIT_PAY, OrderStatus.WAIT_SHIP, OperatorType.SYSTEM, null,
                "支付成功（%s）".formatted(channel == null ? "未知渠道" : channel.getLabel()));
        log.info("订单支付成功 orderNo={} channel={}", orderNo, channel);
    }

    /**
     * 关闭超时未支付订单。
     *
     * <p>每单先做条件更新再回补库存，语义与取消订单一致。
     * 这里整批放在一个事务里——真实业务该逐单事务（或用 MQ 延时消息），
     * 否则一批 100 单卡住会长时间持锁。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpired() {
        // 一次最多捞 100 单，避免单次任务无限期跑
        List<Order> expired = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, OrderStatus.WAIT_PAY)
                .lt(Order::getExpireTime, LocalDateTime.now())
                .orderByAsc(Order::getExpireTime)
                .last("limit 100"));

        int closed = 0;
        for (Order order : expired) {
            // 条件更新兜底：捞出来到改之间可能刚好被支付了
            int rows = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                    .eq(Order::getId, order.getId())
                    .eq(Order::getStatus, OrderStatus.WAIT_PAY)
                    .set(Order::getStatus, OrderStatus.CLOSED)
                    .set(Order::getCloseTime, LocalDateTime.now()));

            // 没改到说明状态已经变了，跳过
            if (rows == 0) {
                continue;
            }

            // 关单成功才回补库存
            for (OrderItem item : listItems(order.getId())) {
                skuStockService.restore(item.getSkuId(), item.getQuantity());
            }

            writeLog(order, OrderStatus.WAIT_PAY, OrderStatus.CLOSED, OperatorType.SYSTEM, null,
                    "超时未支付自动关闭");
            closed++;
        }

        if (closed > 0) {
            log.info("超时关单完成，本次关闭 {} 单", closed);
        }
        return closed;
    }

    /**
     * 按单号 + 用户双条件取订单。
     *
     * @throws BizException 不存在或不属于该用户，统一 13002（不泄露存在性）
     */
    private Order getOwned(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));

        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 只按单号取（供支付回调使用，归属已由支付单保证）。 */
    private Order getByOrderNo(String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo));

        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 查订单明细。 */
    private List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    /**
     * 写一条状态流水。
     *
     * <p>只追加不修改：客服判责、对账排查全靠这张表回答
     * "谁在什么时候把状态从什么改成了什么"。
     */
    private void writeLog(Order order, OrderStatus from, OrderStatus to,
                          OperatorType operatorType, Long operatorId, String remark) {
        OrderLog row = new OrderLog();
        row.setOrderId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setFromStatus(from);
        row.setToStatus(to);
        row.setOperatorType(operatorType);
        row.setOperatorId(operatorId);
        row.setRemark(remark);
        orderLogMapper.insert(row);
    }

    /**
     * 生成订单号：O + 秒级时间 + 4 位随机。
     *
     * <p>为什么不用雪花 ID 对外：雪花 ID 会暴露创建时刻与机器号，
     * 而且太长不好念。业务单号与主键分离是常规做法，唯一性由 {@code uk_order_no} 保证。
     */
    private String nextOrderNo() {
        return "O" + LocalDateTime.now().format(ORDER_NO_TIME) + "%04d".formatted(RANDOM.nextInt(10000));
    }

    /**
     * 把地址压成 JSON 快照存进订单。
     *
     * <p>存 JSON 而不是拆成 4 个列：收货信息字段以后可能增加（如自提点、门牌号），
     * 快照本身是一次性的历史数据，不需要被检索，用 JSON 最省事。
     */
    private String toSnapshot(AddressVO address) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("receiver", address.receiver());
        snapshot.put("phone", address.phone());
        snapshot.put("address", address.fullAddress());
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (Exception e) {
            // 理论上不会发生（全是字符串），兜底成系统错误
            throw new BizException(ResultCode.SYSTEM_ERROR, "收货信息序列化失败");
        }
    }

    /**
     * 快照 JSON → 展示用摘要「张三 138xxxx 湖北省武汉市…」。
     *
     * <p>解析失败就原样返回：老数据格式变了也不该让整页 500。
     */
    private String summaryOf(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return "";
        }
        try {
            Map<String, String> m = OBJECT_MAPPER.readValue(snapshot, new TypeReference<Map<String, String>>() {
            });
            return "%s %s %s".formatted(
                    m.getOrDefault("receiver", ""),
                    m.getOrDefault("phone", ""),
                    m.getOrDefault("address", "")).trim();
        } catch (Exception e) {
            return snapshot;
        }
    }

    /**
     * 解析状态参数，空值返回 null（表示不过滤）。
     *
     * @throws BizException 非法状态 40000
     */
    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "订单状态不合法");
        }
    }
}
