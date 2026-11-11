package com.smartmall.order.enums;

public enum AfterSaleType {
    REFUND_ONLY("仅退款"),
    RETURN_REFUND("退货退款");

    private final String label;

    AfterSaleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
