package com.smartmall.catalog.vo;


import com.smartmall.catalog.domain.Sku;

import java.math.BigDecimal;

/**
 * SKU 对外视图。
 *
 * @param id      规格 ID，下单时传的是它
 * @param spec    规格描述，如「8GB+128GB 曜石黑」
 * @param price   该规格售价
 * @param stock   可售库存
 * @param image   规格图
 * @param enabled 是否可售
 */
public record SkuVO(Long id, String spec, BigDecimal price, int stock, String image, boolean enabled) {

    public static SkuVO of(Sku sku) {
        return new SkuVO(sku.getId(), sku.getSpec(), sku.getPrice(), sku.getStock(),
                sku.getImage(), sku.isEnabled());
    }
}
