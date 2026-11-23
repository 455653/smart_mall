package com.smartmall.catalog.controller;

import com.smartmall.catalog.dto.ProductQuery;
import com.smartmall.catalog.service.ProductService;
import com.smartmall.catalog.vo.ProductDetailVO;
import com.smartmall.catalog.vo.ProductVO;
import com.smartmall.common.api.PageResult;
import com.smartmall.common.api.Result;
import com.smartmall.common.security.RequireRole;
import com.smartmall.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 后台商品管理接口层。
 *
 * <p><b>接口清单</b>：
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>GET</td><td>/api/admin/products</td><td>商品列表（含下架与草稿）</td></tr>
 *   <tr><td>GET</td><td>/api/admin/products/{id}</td><td>商品详情（下架也能看）</td></tr>
 *   <tr><td>PUT</td><td>/api/admin/products/{id}/status?onSale=</td><td>上架 / 下架</td></tr>
 * </table>
 *
 * <p><b>鉴权</b>：{@code @RequireRole({OPERATOR, ADMIN})} 标在类上，语义是「满足其一即可」。
 * 客服与顾客访问一律 40300。
 */
@RestController                                    // REST 控制器
@RequestMapping("/api/admin/products")             // 公共前缀：后台商品
@RequireRole({Role.OPERATOR, Role.ADMIN})          // 类级鉴权：运营或管理员
@RequiredArgsConstructor                           // 构造器注入
public class AdminProductController {

    // 商品服务：与前台是同一个实现，只是调用不同的方法
    private final ProductService productService;

    /** 后台商品列表：不过滤上下架状态，下架商品、草稿也会出现在结果里。 */
    @GetMapping
    public Result<PageResult<ProductVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {

        var query = new ProductQuery(keyword, categoryId, brandId, minPrice, maxPrice,
                sort, pageNum, pageSize);

        // 调后台检索：与前台的区别只有「不加上架状态过滤」
        return Result.ok(productService.adminPage(query));
    }

    /** 后台商品详情：下架商品也能看，便于编辑；不走缓存，运营看到的必须是最新数据。 */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.adminDetail(id));
    }

    /**
     * 上架 / 下架。
     *
     * <p>参数用 query 而不是请求体：只有一个布尔值，{@code ?onSale=false} 最直观。
     * 不传 {@code onSale} 会返回 400（缺少必填参数），而不是默默当成 false。
     *
     * <p><b>副作用</b>：会整体清空商品详情缓存。
     */
    @PutMapping("/{id}/status")
    public Result<ProductVO> changeStatus(@PathVariable Long id,
                                          @RequestParam boolean onSale) {
        return Result.ok(productService.changeStatus(id, onSale));
    }
}
