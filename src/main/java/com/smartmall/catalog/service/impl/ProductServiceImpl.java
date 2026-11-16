package com.smartmall.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartmall.catalog.domain.Brand;
import com.smartmall.catalog.domain.Category;
import com.smartmall.catalog.domain.Product;
import com.smartmall.catalog.domain.Sku;
import com.smartmall.catalog.dto.ProductQuery;
import com.smartmall.catalog.mapper.BrandMapper;
import com.smartmall.catalog.mapper.CategoryMapper;
import com.smartmall.catalog.mapper.ProductMapper;
import com.smartmall.catalog.mapper.SkuMapper;
import com.smartmall.catalog.repository.ProductQueries;
import com.smartmall.catalog.service.CategoryService;
import com.smartmall.catalog.service.ProductService;
import com.smartmall.catalog.vo.ProductDetailVO;
import com.smartmall.catalog.vo.ProductVO;
import com.smartmall.catalog.vo.SkuVO;
import com.smartmall.common.api.PageResult;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j                     // 生成 log
@Service                   // 注册为业务组件
@RequiredArgsConstructor   // 构造器注入
public class ProductServiceImpl implements ProductService {

    // 商品表 Mapper：SPU 的查询与更新
    private final ProductMapper productMapper;

    private final CategoryMapper categoryMapper;

    private final BrandMapper brandMapper;
    private final SkuMapper skuMapper;
    // 分类服务：注入接口而不是实现类，以后换实现本类不用改
    private final CategoryService categoryService;

    @Override
    public PageResult<ProductVO> page(ProductQuery query) {

        // 分类条件：不传就是"不限"；传了就展开成"它 + 所有子分类"的 ID
        List<Long> categoryIds = null;
        if (query.categoryId() != null) {
            categoryIds = subtreeIds(query.categoryId());
        }

        // 拼查询条件：true 表示前台只查上架商品
        LambdaQueryWrapper<Product> wrapper =
                ProductQueries.ofQuery(query, categoryIds, true);

        // 排序：sort 传了非法值不会报错，会自动落回默认排序
        ProductQueries.sorted(query.normalizeSort(), wrapper);

        // 执行分页查询：分页插件自动拼 limit 并额外发一条 count
        Page<Product> productPage =
                productMapper.selectPage(new Page<>(query.page(), query.size()), wrapper);

        // 实体分页转 VO 分页：避免把数据库实体（含内部字段）直接暴露给前端
        return PageResult.of(productPage, ProductVO::of);
    }

    /**
     * 递归收集子分类 ID。
     *
     * @param parentId 当前分类 ID
     * @param all      全部分类
     * @param result   结果容器，边找边往里加
     */
    private void collectChildren(Long parentId, List<Category> all, List<Long> result) {
        // 在全部分类里找"父亲是 parentId"的分类
        for (Category category : all) {
            if (parentId.equals(category.getParentId())) {
                // 找到就收进结果
                result.add(category.getId());
                // 再以它为起点继续往下找，直到没有子分类为止
                collectChildren(category.getId(), all, result);
            }
        }
    }

    /**
     * 收集某个分类 + 它下面所有子孙分类的 ID。
     *
     * <p>用途：用户点「数码家电」时，商品其实挂在「手机通讯」下，
     * 不把子分类 ID 一起带上就一件商品也搜不到。
     */
    @Override
    public List<Long> subtreeIds(Long categoryId) {

        // 没有分类条件时返回空列表
        if (categoryId == null) {
            return List.of();
        }
        // 一次查出全部分类（分类表很小，直接全查最省事）
        List<Category> all = categoryMapper.selectList(null);

        List<Long> result = new ArrayList<>();
        result.add(categoryId);

        // 递归把子孙的 id 全部加进来
        collectChildren(categoryId, all, result);

        return result;
    }

    // 详情：查商品 + SKU + 品牌 + 面包屑，拼成 VO 后进缓存
    @Override
    public ProductDetailVO detail(Long productId) {

        // 先查商品，查不到直接抛业务异常，避免后续 product.getXxx() 空指针
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // 该商品的在售 SKU，价格低的在前
        List<SkuVO> skuList = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                .eq(Sku::getProductId, productId)
                .eq(Sku::isEnabled, true)
                .orderByAsc(Sku::getPrice)             // 价格低的在前
                .orderByAsc(Sku::getId)).stream().map(SkuVO::of).toList();

        String brandName = "平台自营";
        if (product.getBrandId() != null) {
            Brand brand = brandMapper.selectById(product.getBrandId());
            if (brand != null) {
                brandName = brand.getName();
            }
        }

        // sku 列表 + 品牌名 + 商品 + 面包屑(暂缺) 拼成详情 VO
        return ProductDetailVO.of(product, null, brandName, skuList);
    }
}
