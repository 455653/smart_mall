package com.smartmall.order.enums;

public enum OperatorType {
    USER("用户"),
    ADMIN("管理员"),
    SYSTEM("系统");

    private final String label;

    OperatorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
