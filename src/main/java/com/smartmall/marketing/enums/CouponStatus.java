package com.smartmall.marketing.enums;
/** 用户券状态。库里存 name()，如 UNUSED。 */
public enum CouponStatus {

    UNUSED("未使用"),
    USED("已使用"),
    EXPIRED("已过期");

    private final String label;

    CouponStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
