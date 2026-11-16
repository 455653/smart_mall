package com.smartmall.catalog.dto;

import java.math.BigDecimal;

/**
 * 商品检索条件。
 *
 * <p>所有字段都可空，缺省即"不限"。这里只做兜底与规整，
 * 真正的条件拼接在 {@code ProductQueries} 里。
 *
 * <p>为什么 {@code page}/{@code size} 要兜底：前端不传、传 0、传 -1、传 100000
 * 都会发生，直接把用户输入当 limit 用（{@code limit 100000}）就是一次拖库。
 *
 * @param keyword    关键词，同时匹配标题与副标题
 * @param categoryId 分类 ID，传一级分类时自动含所有子分类
 * @param brandId    品牌 ID
 * @param minPrice   价格下限
 * @param maxPrice   价格上限
 * @param sort       排序方式，白名单见 {@code ProductQueries#sorted}
 * @param pageNum    页码，从 1 开始
 * @param pageSize   每页条数
 */
public record ProductQuery(String keyword, Long categoryId, Long brandId,
                           BigDecimal minPrice, BigDecimal maxPrice, String sort,
                           Integer pageNum, Integer pageSize) {

    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 60;

    public int page() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public int size() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(pageSize, MAX_SIZE);
    }

    public String normalizeSort() {
        return sort == null || sort.isBlank() ? "default" : sort.trim();
    }
}
