package com.smartmall.ai.enums;

/** AI 会话状态。库里存 name()，如 AI。 */
public enum SessionStatus {

    AI("AI 接待"),
    HUMAN("转人工"),
    CLOSED("已关闭");

    private final String label;

    SessionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
