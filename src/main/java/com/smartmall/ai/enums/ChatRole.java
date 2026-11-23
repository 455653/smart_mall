package com.smartmall.ai.enums;

/** 会话消息的发言方。库里存 name()，如 USER。 */
public enum ChatRole {

    USER("用户"),
    ASSISTANT("AI"),
    SYSTEM("系统");

    private final String label;

    ChatRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
