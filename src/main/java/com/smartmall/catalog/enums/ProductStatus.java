package com.smartmall.catalog.enums;

/** 商品上下架状态。库里存 name()，建表默认 OFF_SHELF。 */
public enum ProductStatus {

    ON_SALE("在售"),
    OFF_SHELF("已下架");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
