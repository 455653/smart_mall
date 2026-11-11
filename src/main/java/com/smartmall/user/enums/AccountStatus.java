package com.smartmall.user.enums;

/** 账号状态。库里存 name()，建表默认 ENABLED。 */
public enum AccountStatus {

    ENABLED("启用"),
    DISABLED("禁用");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
