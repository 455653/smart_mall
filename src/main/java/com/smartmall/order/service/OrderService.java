package com.smartmall.order.service;

import com.smartmall.common.api.PageResult;
import com.smartmall.common.exception.BizException;
import com.smartmall.order.domain.Order;
import com.smartmall.order.dto.CreateOrderRequest;
import com.smartmall.order.vo.OrderVO;
import com.smartmall.pay.enums.PayChannel;

/**
 * 订单服务接口。
 *
 * <p>交易链路的三条硬规则：
 * <ol>
 *   <li><b>价格以快照为准</b>：明细冗余商品名/规格/单价，事后改价不影响历史单</li>
 *   <li><b>库存靠条件更新扣</b>：不「先查再判」，把 {@code stock >= n} 写进 UPDATE 的 where</li>
 *   <li><b>状态只走状态机</b>：非法跳转一律 13003，绝不允许「已取消被发货」</li>
 * </ol>
 *
 * <p>对外暴露 {@link #requireOrder} 与 {@link #markPaid} 两个方法，是给 pay 包用的：
 * 支付服务需要读订单金额、并在回调成功后推进订单状态，但<b>不许碰 order 的 mapper</b>。
 */
public interface OrderService {

    /**
     * 下单：读购物车勾选项 → 扣库存 → 写订单/明细/流水 → 清掉已结算的购物车条目。
     *
     * <p>整段在一个事务里：任何一步失败（库存不足、地址不存在）全部回滚，
     * 不会出现「扣了库存却没生成订单」的中间态。
     *
     * @param userId  当前用户 ID
     * @param request 下单请求（地址 + 幂等键）
     * @return 新建订单
     * @throws BizException 购物车为空 13001；地址不存在 40400；库存不足 12003
     */
    OrderVO create(Long userId, CreateOrderRequest request);

    /**
     * 我的订单分页。
     *
     * <p>列表不返回明细：订单列表只展示「共 N 件 + 总金额」，逐单查明细是 N+1。
     *
     * @param userId   当前用户 ID
     * @param status   状态过滤，null 或空串表示全部
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<OrderVO> page(Long userId, String status, long pageNum, long pageSize);

    /**
     * 订单详情（含明细）。
     *
     * @param userId  当前用户 ID
     * @param orderNo 订单号
     * @return 订单详情
     * @throws BizException 订单不存在或不属于当前用户 13002
     */
    OrderVO detail(Long userId, String orderNo);

    /**
     * 取消订单：仅待支付可取消，取消后回补库存。
     *
     * @param userId  当前用户 ID
     * @param orderNo 订单号
     * @throws BizException 订单不存在 13002；状态不允许 13003
     */
    void cancel(Long userId, String orderNo);

    /**
     * 按单号取订单并校验归属，供支付服务创建支付单。
     *
     * @param orderNo 订单号
     * @param userId  当前用户 ID
     * @return 订单实体
     * @throws BizException 订单不存在或不属于该用户 13002
     */
    Order requireOrder(String orderNo, Long userId);

    /**
     * 支付成功，把订单从待支付推进到待发货。
     *
     * <p><b>幂等</b>：重复调用（第三方重复回调）第二次影响 0 行，
     * 此时看当前状态——已是待发货就直接返回，不再报错。
     *
     * @param orderNo 订单号
     * @param channel 支付渠道，回填到订单的 pay_type
     * @throws BizException 订单已关闭 13004（此时钱已付，需走退款）
     */
    void markPaid(String orderNo, PayChannel channel);

    /**
     * 关闭所有超时未支付的订单，回补库存。
     *
     * <p>由定时任务每分钟调一次。
     *
     * @return 本次关闭的订单数
     */
    int closeExpired();
}
