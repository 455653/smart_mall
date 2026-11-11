package com.smartmall.marketing.enums;

public enum CouponType {

    FULL_REDUCTION("满减券"),
    DISCOUNT("折扣券");
    private final String label;

    CouponType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
