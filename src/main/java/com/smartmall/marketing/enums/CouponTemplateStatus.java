package com.smartmall.marketing.enums;

public enum CouponTemplateStatus {
    ACTIVE("生效中"),
    PAUSED("已暂停"),
    EXPIRED("已结束");

    private final String label;

    CouponTemplateStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
