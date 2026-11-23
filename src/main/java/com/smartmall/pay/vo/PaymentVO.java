package com.smartmall.pay.vo;

import com.smartmall.pay.domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单视图。
 *
 * <p>{@code tradeNo} 是第三方交易号，退款与对账都以它为准；
 * 未收到回调前为 null，前端据此显示"等待支付"。
 *
 * @param paymentNo    支付单号，调起第三方收银台时作为商户订单号
 * @param orderNo      关联订单号
 * @param amount       支付金额，恒等于订单实付金额
 * @param status       状态枚举名，如 PENDING
 * @param statusLabel  状态中文
 * @param channel      渠道枚举名
 * @param channelLabel 渠道中文
 * @param tradeNo      第三方交易号
 * @param callbackTime 回调时间
 * @param createTime   创建时间
 */
public record PaymentVO(String paymentNo, String orderNo, BigDecimal amount,
                        String status, String statusLabel,
                        String channel, String channelLabel,
                        String tradeNo, LocalDateTime callbackTime, LocalDateTime createTime) {

    /** 实体 → VO */
    public static PaymentVO of(Payment payment) {
        return new PaymentVO(
                payment.getPaymentNo(),
                payment.getOrderNo(),
                payment.getAmount(),
                payment.getStatus() == null ? null : payment.getStatus().name(),
                payment.getStatus() == null ? null : payment.getStatus().getLabel(),
                payment.getChannel() == null ? null : payment.getChannel().name(),
                payment.getChannel() == null ? null : payment.getChannel().getLabel(),
                payment.getTradeNo(),
                payment.getCallbackTime(),
                payment.getCreateTime());
    }
}
