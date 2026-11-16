package com.smartmall.catalog.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.catalog.domain.Product;
import com.smartmall.catalog.dto.ProductQuery;
import com.smartmall.catalog.enums.ProductStatus;

import java.util.List;

/**
 * 商品检索条件拼装。
 *
 * <p>为什么不用字符串拼 SQL：{@code LambdaQueryWrapper} 用 {@code Product::getTitle}
 * 这类<b>方法引用</b>定位列名，值走参数化占位符，天然防注入；改名时编译期就能发现，
 * 也不会出现 {@code where 1=1 and ...} 这种手工拼接。
 *
 * <p>逻辑删除由 {@code @TableLogic} 统一处理，这里不用写 {@code deleted = 0}。
 */
public final class ProductQueries {

    private ProductQueries() {
    }

    /** 前台只查上架商品 */
    public static LambdaQueryWrapper<Product> onSale() {
        return Wrappers.<Product>lambdaQuery().eq(Product::getStatus, ProductStatus.ON_SALE);
    }

    /**
     * 按查询条件拼装。
     *
     * @param query       检索条件
     * @param categoryIds 分类 ID 集合，传一级分类时应含所有子孙分类
     * @param onlyOnSale  是否只查上架商品（后台传 false）
     */
    public static LambdaQueryWrapper<Product> ofQuery(ProductQuery query,
                                                      List<Long> categoryIds,
                                                      boolean onlyOnSale) {
        LambdaQueryWrapper<Product> wrapper = Wrappers.lambdaQuery();
        if (onlyOnSale) {
            wrapper.eq(Product::getStatus, ProductStatus.ON_SALE);
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            wrapper.in(Product::getCategoryId, categoryIds);
        }
        if (query.brandId() != null) {
            wrapper.eq(Product::getBrandId, query.brandId());
        }

        String keyword = query.keyword() == null ? "" : query.keyword().trim();
        if (!keyword.isEmpty()) {
            // 必须用 and(...) 把 or 包成一组，否则会变成
            // "status = ON_SALE or title like ..." —— 下架商品就漏出来了
            wrapper.and(w -> w.like(Product::getTitle, keyword)
                    .or().like(Product::getSubtitle, keyword));
        }
        if (query.minPrice() != null) {
            wrapper.ge(Product::getPrice, query.minPrice());
        }
        if (query.maxPrice() != null) {
            wrapper.le(Product::getPrice, query.maxPrice());
        }
        return wrapper;
    }

    /**
     * 排序白名单。
     *
     * <p>直接把前端传的 {@code sort} 拼进 {@code order by} 是经典漏洞——攻击者可以按任意列
     * （包括未展示的、未索引的列）排序，既泄露信息又能拖垮数据库。这里用 switch 做映射，
     * <b>非法值不报错、落到默认排序</b>：乱传 {@code sort=drop} 只是按默认序返回。
     */
    public static LambdaQueryWrapper<Product> sorted(String sort, LambdaQueryWrapper<Product> wrapper) {
        return switch (sort) {
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice);
            case "rating_desc" -> wrapper.orderByDesc(Product::getRating);
            case "new" -> wrapper.orderByDesc(Product::getCreateTime);
            case "sales_desc" -> wrapper.orderByDesc(Product::getSales);
            default -> wrapper.orderByDesc(Product::getSales).orderByDesc(Product::getId);
        };
    }
}
