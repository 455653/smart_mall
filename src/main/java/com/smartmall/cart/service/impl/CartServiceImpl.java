package com.smartmall.cart.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.catalog.domain.Product;
import com.smartmall.catalog.domain.Sku;
import com.smartmall.catalog.service.ProductService;
import com.smartmall.catalog.service.SkuStockService;
import com.smartmall.cart.dto.AddCartItemRequest;
import com.smartmall.cart.dto.UpdateCartItemRequest;
import com.smartmall.cart.service.CartService;
import com.smartmall.cart.vo.CartItemVO;
import com.smartmall.cart.vo.CartVO;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;
import com.smartmall.order.domain.CartItem;
import com.smartmall.order.mapper.CartItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务实现。
 *
 * <p>核心取舍：<b>购物车只存「想买什么」，不存「多少钱」</b>。
 * {@code oms_cart_item} 里只有 sku_id 和数量，价格每次从 catalog 实时取。
 *
 * <p>失效商品不删、只标记：规格下架或售罄时，条目仍展示但 {@code available=false}，
 * 由用户自己决定删还是等补货。后端替用户删购物车是「擅自处置用户数据」。
 *
 * <p>跨包纪律：本类只注入 catalog 的 {@code ProductService} / {@code SkuStockService}
 * 两个<b>接口</b>，不注入任何 {@code catalog.mapper}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    // 购物车表 Mapper：单表 CRUD
    private final CartItemMapper cartItemMapper;

    // 库存服务：查规格（拿价格/库存/规格文案）
    private final SkuStockService skuStockService;

    // 商品服务：查标题与主图
    private final ProductService productService;

    /**
     * 加购：已存在则累加数量。
     *
     * <p>为什么靠「先查再写」而不是「直接 insert 靠唯一键冲突兜底」：
     * 累加语义需要知道原数量，冲突回滚后还得重查一次。
     * {@code uk_cart_user_sku} 仍然保留，作为并发下的最后一道防线。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)          // 查 + 累加 + 更新必须在同一事务
    public CartItemVO add(Long userId, AddCartItemRequest request) {
        // 1. 查规格：不存在直接抛 12004
        Sku sku = skuStockService.getSku(request.skuId());

        // 2. 规格本身能不能卖：停用、售罄、已删除都拦在这里
        if (!sku.hasStock()) {
            throw new BizException(ResultCode.PRODUCT_OFF_SHELF, "该规格已停用或已售罄");
        }

        // 3. 商品 SPU 是否在售（详情页下架后不该还能加购）
        Product product = productService.getProduct(sku.getProductId());
        if (!product.onSale()) {
            throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
        }

        // 4. 本次数量是否超过库存：这里的判断只为提示友好，真正的闸门在扣减 SQL
        if (!sku.hasStock(request.quantity())) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                    "库存不足，当前仅剩 %d 件".formatted(sku.getStock()));
        }

        // 5. 查是否已在车里：user_id + sku_id 双条件，天然只命中自己的行
        CartItem exist = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, request.skuId()));

        if (exist != null) {
            // 6a. 已存在 → 累加，并顺手勾上（用户刚加的东西默认要结算）
            int merged = exist.getQuantity() + request.quantity();
            if (!sku.hasStock(merged)) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                        "库存不足：车里已有 %d 件，当前仅剩 %d 件"
                                .formatted(exist.getQuantity(), sku.getStock()));
            }
            exist.setQuantity(merged);
            exist.setChecked(true);
            cartItemMapper.updateById(exist);
            return toVO(exist);
        }

        // 6b. 不存在 → 新建一条，默认勾选
        CartItem item = new CartItem();
        item.setUserId(userId);                             // 归属只能来自令牌
        item.setProductId(sku.getProductId());              // 从 SKU 反查，不信前端传的
        item.setSkuId(sku.getId());
        item.setQuantity(request.quantity());
        item.setChecked(true);
        cartItemMapper.insert(item);

        log.debug("加购成功 userId={} skuId={} qty={}", userId, sku.getId(), request.quantity());
        return toVO(item);
    }

    /**
     * 购物车列表 + 结算汇总。
     *
     * <p>逐条回查 catalog 是 N+1 查询，但购物车条目数是个位数量级，
     * 换来的是「价格永远是最新的」。
     */
    @Override
    public CartVO list(Long userId) {
        // 只查自己的车，新加的在前面
        List<CartItem> rows = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));

        List<CartItemVO> items = new ArrayList<>(rows.size());
        int totalQuantity = 0;                                  // 角标：所有条目数量之和
        int checkedQuantity = 0;                                // 勾选且可买的数量
        BigDecimal checkedAmount = BigDecimal.ZERO;             // 勾选且可买的金额

        for (CartItem row : rows) {
            CartItemVO vo = toVO(row);
            items.add(vo);
            totalQuantity += row.getQuantity();

            // 只有「勾选 + 还买得了」才计入结算金额
            if (vo.checked() && vo.available()) {
                checkedQuantity += vo.quantity();
                checkedAmount = checkedAmount.add(vo.subtotal());
            }
        }

        // 全选框状态：非空且每条都勾了才算全选
        boolean allChecked = !items.isEmpty() && items.stream().allMatch(CartItemVO::checked);

        return new CartVO(items, totalQuantity, checkedQuantity, checkedAmount, allChecked);
    }

    /**
     * 改数量 / 改勾选。
     *
     * <p>改数量时重新校验库存：用户可能把车放了一周，库存早被买空了。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartItemVO update(Long userId, Long id, UpdateCartItemRequest request) {
        // 双条件取条目：拿不到就是不存在或不是你的（两种情况同一个码，不泄露存在性）
        CartItem item = getOwned(userId, id);

        // 改数量：先确认这个数量现在还买得了
        if (request.quantity() != null) {
            Sku sku = skuStockService.getSku(item.getSkuId());
            if (!sku.hasStock(request.quantity())) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                        "库存不足，当前仅剩 %d 件".formatted(sku.getStock()));
            }
            item.setQuantity(request.quantity());
        }

        // 改勾选：独立判断，两个字段互不影响
        if (request.checked() != null) {
            item.setChecked(request.checked());
        }

        cartItemMapper.updateById(item);
        return toVO(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long id) {
        // 先校验归属，再删；顺序反过来就是「先删了才发现不是自己的」
        getOwned(userId, id);
        cartItemMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear(Long userId) {
        // 逻辑删除：MP 会把 delete 改写成 update deleted = 1
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAll(Long userId, boolean checked) {
        // 一条 update 搞定，不用先查出来再逐条改
        cartItemMapper.update(null, Wrappers.<CartItem>lambdaUpdate()
                .eq(CartItem::getUserId, userId)
                .set(CartItem::isChecked, checked));
    }

    @Override
    public List<CartItem> listChecked(Long userId) {
        // 只返回勾选的，按加购顺序（下单明细按用户加购顺序更好读）
        return cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::isChecked, true)
                .orderByAsc(CartItem::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByIds(Long userId, List<Long> ids) {
        // 空列表不执行 SQL：MyBatis-Plus 的 in() 传空集合会拼出 in ()，语法错误
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 带上 user_id：调用方传错 ID 也删不到别人的车
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getId, ids));
    }

    /**
     * 取当前用户名下的条目，双条件查询。
     *
     * @throws BizException 条目不存在或不属于该用户，统一抛 13006
     */
    private CartItem getOwned(Long userId, Long id) {
        CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getId, id)
                .eq(CartItem::getUserId, userId));

        // 查不到有两种可能：真没这条，或是别人的。对外统一成「不存在」
        if (item == null) {
            throw new BizException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        return item;
    }

    /**
     * 实体 → VO，顺带补齐商品信息。
     *
     * <p>商品被删除或规格停用时<b>不抛异常</b>，而是返回一条 {@code available=false}
     * 的 VO：购物车里有失效商品是常态，整页报错会让用户连能买的都结不了账。
     */
    private CartItemVO toVO(CartItem row) {
        try {
            Sku sku = skuStockService.getSku(row.getSkuId());
            Product product = productService.getProduct(row.getProductId());

            // 单价取 SKU 现价；SPU 上的 price 只是「最低价」冗余，不能用作结算价
            BigDecimal price = sku.getPrice() == null ? BigDecimal.ZERO : sku.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(row.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            // 可买 = 规格启用有货 + 商品在售 + 库存够本次数量
            boolean available = sku.hasStock() && product.onSale() && sku.hasStock(row.getQuantity());

            return new CartItemVO(row.getId(), row.getProductId(), row.getSkuId(),
                    product.getTitle(), sku.getSpec(), product.getMainImage(),
                    price, row.getQuantity(), row.isChecked(), sku.getStock(), available, subtotal);
        } catch (BizException e) {
            // 规格或商品已经被删：条目留着但标失效，只展示 ID 与数量
            return new CartItemVO(row.getId(), row.getProductId(), row.getSkuId(),
                    "商品已失效", null, null,
                    BigDecimal.ZERO, row.getQuantity(), row.isChecked(), 0, false, BigDecimal.ZERO);
        }
    }
}
