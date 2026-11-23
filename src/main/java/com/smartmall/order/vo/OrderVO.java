package com.smartmall.order.vo;

import com.smartmall.order.domain.Order;
import com.smartmall.order.domain.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图。
 *
 * <p>金额分四项返回而不是只给一个总数：用户要知道「商品多少钱、优惠了多少、运费多少」，
 * 客服处理退款也要按这四项拆。只返回实付金额，日后对账时无从追溯。
 *
 * @param orderNo        订单号（对外唯一标识，不暴露雪花 ID）
 * @param status         状态枚举名，如 WAIT_PAY
 * @param statusLabel    状态中文，前端直接展示
 * @param totalAmount    商品总额
 * @param discountAmount 优惠合计（当前未接入营销，恒为 0）
 * @param freightAmount  运费
 * @param payAmount      实付金额，支付单直接取它
 * @param itemCount      商品件数
 * @param receiver       收货信息摘要，由快照 JSON 拼成
 * @param expireTime     支付截止时间
 * @param createTime     下单时间
 * @param payTime        支付时间，未支付为 null
 * @param items          明细；列表接口不返回，为空列表
 */
public record OrderVO(String orderNo, String status, String statusLabel,
                      BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal freightAmount,
                      BigDecimal payAmount, int itemCount, String receiver,
                      LocalDateTime expireTime, LocalDateTime createTime, LocalDateTime payTime,
                      List<OrderItemVO> items) {

    /**
     * 实体 → VO。
     *
     * @param order    订单主表
     * @param items    明细，列表场景传空列表
     * @param receiver 收货信息摘要
     */
    public static OrderVO of(Order order, List<OrderItem> items, String receiver) {
        return new OrderVO(
                order.getOrderNo(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getStatus() == null ? null : order.getStatus().getLabel(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFreightAmount(),
                order.getPayAmount(),
                order.getItemCount(),
                receiver,
                order.getExpireTime(),
                order.getCreateTime(),
                order.getPayTime(),
                items == null ? List.of() : items.stream().map(OrderItemVO::of).toList());
    }
}
