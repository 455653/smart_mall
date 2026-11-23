package com.smartmall.pay.dto;

import com.smartmall.pay.enums.PayChannel;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建支付单请求。
 *
 * <p>不传金额：金额从订单的 {@code pay_amount} 取。让前端传金额，
 * 等于把"付多少钱"的决定权交给客户端——这是支付环节最经典的漏洞。
 *
 * @param orderNo 订单号
 * @param channel 支付渠道，不传默认 MOCK（演示用）
 */
public record CreatePaymentRequest(

        @NotBlank(message = "订单号不能为空")
        String orderNo,

        PayChannel channel
) {
}
