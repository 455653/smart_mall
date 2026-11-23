package com.smartmall.ai.enums;

/** 知识库文档的向量化状态。库里存 name()，如 PENDING。 */
public enum DocStatus {

    PENDING("待向量化"),
    READY("就绪"),
    FAILED("失败");

    private final String label;

    DocStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
