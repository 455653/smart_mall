package com.smartmall.pay.enums;

/**
 * 支付单状态。
 */
public enum PaymentStatus {
    PENDING("待支付"),
    PAID("支付成功"),
    FAILED("支付失败"),
    REFUNDED("已退款");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
