package com.smartmall.order.vo;

import com.smartmall.order.domain.OrderItem;

import java.math.BigDecimal;

/**
 * 订单明细视图（商品快照）。
 *
 * <p>这里的标题、规格、单价全部来自 {@code oms_order_item} 的冗余字段，
 * <b>不回查商品表</b>：商品改价、改名、下架之后，历史订单仍要能还原当时买的是什么。
 *
 * @param productId    商品 SPU
 * @param skuId        规格 ID
 * @param productTitle 下单时的商品标题
 * @param skuSpec      下单时的规格
 * @param productImage 下单时的主图
 * @param price        下单时的单价
 * @param quantity     数量
 * @param totalAmount  小计
 */
public record OrderItemVO(Long productId, Long skuId, String productTitle, String skuSpec,
                          String productImage, BigDecimal price, int quantity, BigDecimal totalAmount) {

    /** 实体 → VO，纯字段搬运，不做任何查询 */
    public static OrderItemVO of(OrderItem item) {
        return new OrderItemVO(item.getProductId(), item.getSkuId(), item.getProductTitle(),
                item.getSkuSpec(), item.getProductImage(), item.getPrice(),
                item.getQuantity(), item.getTotalAmount());
    }
}
