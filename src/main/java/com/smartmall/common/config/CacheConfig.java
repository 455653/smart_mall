package com.smartmall.common.config;

/**
 * 缓存名常量。
 *
 * <p>集中管理所有 @Cacheable / @CacheEvict 用到的 cache 名，
 * 避免在各处散落字符串字面量导致拼错或改名时漏改。
 */
public final class CacheConfig {

    private CacheConfig() {
    }

    /** 分类树缓存：全站共用一份，key 固定为 'all'。 */
    public static final String CATEGORY_TREE = "category:tree";

    /** 商品详情缓存：key 为商品 ID。 */
    public static final String PRODUCT_DETAIL = "product:detail";
}
