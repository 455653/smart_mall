package com.smartmall.catalog.controller;

import com.smartmall.catalog.dto.ProductQuery;
import com.smartmall.catalog.service.ProductService;
import com.smartmall.catalog.vo.ProductVO;
import com.smartmall.common.api.PageResult;
import com.smartmall.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor  // 构造器注入
public class ProductController {

    // 商品服务：接口类型，便于替换实现
    private final ProductService productService;

    /** 商品分页检索 */
    @GetMapping
    public Result<PageResult<ProductVO>> page(
            // 关键词，required = false 表示可以不传（不传就是 null）
            @RequestParam(required = false) String keyword,
            // 分类 ID，不传则不限分类
            @RequestParam(required = false) Long categoryId,
            // 品牌 ID，不传则不限品牌
            @RequestParam(required = false) Long brandId,
            // 价格下限，用 BigDecimal 接收，避免 2999.00 变成 2998.9999999
            @RequestParam(required = false) BigDecimal minPrice,
            // 价格上限
            @RequestParam(required = false) BigDecimal maxPrice,
            // 排序方式，非法值由服务层白名单兜底成默认序（销量降序）
            @RequestParam(required = false) String sort,
            // 页码，服务层会兜底成 1
            @RequestParam(required = false) Integer pageNum,
            // 每页条数，服务层会兜底成 12、上限 60
            @RequestParam(required = false) Integer pageSize) {

        var query = new ProductQuery(keyword, categoryId, brandId, minPrice, maxPrice,
                sort, pageNum, pageSize);
        return Result.ok(productService.page(query));
    }
}
