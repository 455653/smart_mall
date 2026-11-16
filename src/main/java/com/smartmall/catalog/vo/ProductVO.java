package com.smartmall.catalog.vo;


import com.smartmall.catalog.domain.Product;

import java.math.BigDecimal;

/**
 * 商品列表视图。
 *
 * <p>列表页只带"一张卡片需要的字段"，详情富文本（MEDIUMTEXT）与图集一概不查：
 * 每页 12 条，把 detail 一起搬出来等于白白搬运几十 KB，还可能拖慢整个分页。
 *
 * @param id           商品 ID
 * @param title        标题
 * @param subtitle     卖点
 * @param mainImage    主图
 * @param price        SPU 冗余最低价，列表排序与价格展示直接用
 * @param sales        销量
 * @param rating       评分
 * @param reviewCount  评价数
 * @param status       上下架状态
 * @param categoryId   分类 ID
 * @param brandId      品牌 ID
 */
public record ProductVO(Long id, String title, String subtitle, String mainImage,
                        BigDecimal price, int sales, BigDecimal rating, int reviewCount,
                        String status, Long categoryId, Long brandId) {

    public static ProductVO of(Product product) {
        return new ProductVO(
                product.getId(),
                product.getTitle(),
                product.getSubtitle(),
                product.getMainImage(),
                product.getPrice(),
                product.getSales(),
                product.getRating(),
                product.getReviewCount(),
                product.getStatus() == null ? null : product.getStatus().name(),
                product.getCategoryId(),
                product.getBrandId());
    }
}
