package com.smartmall.marketing.enums;
/** 秒杀活动状态。库里存 name()，如 PENDING。 */
public enum SeckillStatus {
    PENDING("未开始"),
    ACTIVE("进行中"),
    ENDED("已结束");

    private final String label;

    SeckillStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
