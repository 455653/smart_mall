package com.smartmall.catalog.vo;


import com.smartmall.catalog.domain.Product;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 商品详情视图。
 *
 * <p>详情页要拼四类数据：SPU 本体、分类面包屑、品牌名、可售 SKU 列表。
 * 拼装放在 Service 里做完再缓存，命中缓存时前端拿到的是"可直接渲染"的对象，
 * 不会出现"实体有了但品牌名还没查"的中间态。
 *
 * @param id              商品 ID
 * @param title           标题
 * @param subtitle        卖点
 * @param mainImage       主图
 * @param images          图集（库里逗号分隔，这里拆成数组）
 * @param detail          详情富文本
 * @param aiSellingPoints AI 生成卖点（需运营审核后才展示）
 * @param price           最低价
 * @param sales           销量
 * @param rating          评分
 * @param reviewCount     评价数
 * @param status          上下架状态
 * @param categoryId      分类 ID
 * @param categoryPath    分类面包屑，如「数码家电 / 手机通讯」
 * @param brandId         品牌 ID
 * @param brandName       品牌名，无品牌时为「平台自营」
 * @param skus            可售 SKU 列表，按价格升序
 */
public record ProductDetailVO(Long id, String title, String subtitle, String mainImage,
                              List<String> images, String detail, String aiSellingPoints,
                              BigDecimal price, int sales, BigDecimal rating, int reviewCount,
                              String status, Long categoryId, String categoryPath,
                              Long brandId, String brandName, List<SkuVO> skus) {

    public static ProductDetailVO of(Product product, String categoryPath,
                                     String brandName, List<SkuVO> skus) {
        return new ProductDetailVO(
                product.getId(),
                product.getTitle(),
                product.getSubtitle(),
                product.getMainImage(),
                splitImages(product.getImages()),
                product.getDetail(),
                product.getAiSellingPoints(),
                product.getPrice(),
                product.getSales(),
                product.getRating(),
                product.getReviewCount(),
                product.getStatus() == null ? null : product.getStatus().name(),
                product.getCategoryId(),
                categoryPath,
                product.getBrandId(),
                brandName,
                skus == null ? List.of() : skus);
    }

    private static List<String> splitImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        return Arrays.stream(images.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
