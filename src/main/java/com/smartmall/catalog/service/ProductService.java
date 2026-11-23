package com.smartmall.catalog.service;

import com.smartmall.catalog.domain.Product;
import com.smartmall.catalog.dto.ProductQuery;
import com.smartmall.catalog.vo.ProductDetailVO;
import com.smartmall.catalog.vo.ProductVO;
import com.smartmall.common.api.PageResult;

import java.util.List;

public interface ProductService {

    /** 前台列表：加上架过滤 + 子分类展开 + 排序白名单。 */
    PageResult<ProductVO> page(ProductQuery query);

    /** 商品详情：SPU + SKU + 品牌 + 面包屑。 */
    ProductDetailVO detail(Long productId);

    /** 后台列表：不加上架过滤，运营能看到下架商品与草稿。 */
    PageResult<ProductVO> adminPage(ProductQuery query);

    /** 后台详情：走 detail 的取数逻辑，但绕开缓存（要看最新）。 */
    ProductDetailVO adminDetail(Long productId);

    /** 上架 / 下架：改 status + 整体清空详情缓存。 */
    ProductVO changeStatus(Long productId, boolean onSale);

    /** 刷新最低价：取启用 SKU 的最小价写回 SPU，供列表页排序展示。 */
    void refreshPrice(Long productId);

    /** 单查商品：查不到直接抛 12001，调用方不用判空。 */
    Product getProduct(Long productId);
}
