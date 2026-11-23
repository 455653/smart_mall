package com.smartmall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 加入购物车请求。
 *
 * <p>只传 skuId 和数量：商品 ID 由服务端从 SKU 反查，
 * 让前端传 productId 等于给它一次传错的机会。
 *
 * @param skuId    规格 ID，价格与库存的唯一来源
 * @param quantity 购买数量，1~99
 */
public record AddCartItemRequest(

        @NotNull(message = "skuId 不能为空")
        Long skuId,

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少 1 件")
        @Max(value = 99, message = "单次最多加购 99 件")
        Integer quantity
) {
}
