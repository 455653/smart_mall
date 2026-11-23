package com.smartmall.cart.vo;

import java.math.BigDecimal;

/**
 * 购物车条目视图。
 *
 * <p>价格与库存<b>每次实时从 catalog 取</b>，不存在购物车表里：
 * 购物车存的是「想买什么」，不是「当时多少钱」。用户放在购物车里三天，
 * 价格该跟着商品走，下单时才用当时价格做快照。
 *
 * @param id           购物车条目 ID（改数量、删除都用它，不是 skuId）
 * @param productId    商品 SPU
 * @param skuId        规格 ID
 * @param productTitle 商品标题
 * @param skuSpec      规格描述，如「8GB+128GB 曜石黑」
 * @param productImage 展示图
 * @param price        当前单价
 * @param quantity     数量
 * @param checked      是否勾选结算
 * @param stock        当前可售库存
 * @param available    现在还能不能买：规格启用 + 商品在售 + 库存够本次数量
 * @param subtotal     小计 = 单价 × 数量
 */
public record CartItemVO(Long id, Long productId, Long skuId, String productTitle, String skuSpec,
                         String productImage, BigDecimal price, int quantity, boolean checked,
                         int stock, boolean available, BigDecimal subtotal) {
}
