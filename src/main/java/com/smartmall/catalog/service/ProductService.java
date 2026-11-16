package com.smartmall.catalog.service;

import com.smartmall.catalog.dto.ProductQuery;
import com.smartmall.catalog.vo.ProductDetailVO;
import com.smartmall.catalog.vo.ProductVO;
import com.smartmall.common.api.PageResult;

import java.util.List;

public interface ProductService {

    /** 前台列表：加上架过滤 + 子分类展开 + 排序白名单。 */
    PageResult<ProductVO> page(ProductQuery query);

    /** 收集某个分类 + 它下面所有子孙分类的 ID。 */
    List<Long> subtreeIds(Long categoryId);

    /** 商品详情：SPU + SKU + 品牌 + 面包屑。 */
    ProductDetailVO detail(Long productId);
}
