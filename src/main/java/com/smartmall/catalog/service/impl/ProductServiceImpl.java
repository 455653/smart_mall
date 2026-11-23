package com.smartmall.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartmall.catalog.domain.Brand;
import com.smartmall.catalog.domain.Product;
import com.smartmall.catalog.domain.Sku;
import com.smartmall.catalog.dto.ProductQuery;
import com.smartmall.catalog.enums.ProductStatus;
import com.smartmall.catalog.mapper.BrandMapper;
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
import com.smartmall.common.config.CacheConfig;
import com.smartmall.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j                     // 生成 log
@Service                   // 注册为业务组件
@RequiredArgsConstructor   // 构造器注入
public class ProductServiceImpl implements ProductService {

    /** 商品详情缓存名：命中缓存时不再查库 */
    private static final String PRODUCT_DETAIL = CacheConfig.PRODUCT_DETAIL;

    // 商品表 Mapper：SPU 的查询与更新
    private final ProductMapper productMapper;

    private final BrandMapper brandMapper;
    private final SkuMapper skuMapper;
    // 分类服务：注入接口而不是实现类，以后换实现本类不用改
    private final CategoryService categoryService;

    @Override
    public PageResult<ProductVO> page(ProductQuery query) {

        // 分类条件：不传就是"不限"；传了就展开成"它 + 所有子分类"的 ID
        List<Long> categoryIds = null;
        if (query.categoryId() != null) {
            categoryIds = categoryService.subtreeIds(query.categoryId());
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

    // 详情：查商品 + SKU + 品牌 + 面包屑，拼成 VO 后进缓存
    @Override
    @Cacheable(cacheNames = PRODUCT_DETAIL, key = "#productId")   // 结果进缓存，key 为商品 ID
    public ProductDetailVO detail(Long productId) {

        // 先查商品，查不到直接抛业务异常，避免后续 product.getXxx() 空指针
        Product product = getProduct(productId);

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

        // sku 列表 + 品牌名 + 商品 + 面包屑 拼成详情 VO
        return ProductDetailVO.of(product, categoryService.pathOf(product.getCategoryId()), brandName, skuList);
    }

    /**
     * 后台商品列表：包含下架与草稿，供运营维护。
     *
     * <p>与前台唯一的差别是 {@code onlyOnSale = false}。
     */
    @Override
    public PageResult<ProductVO> adminPage(ProductQuery query) {

        // 分类条件处理与前台完全一致：一级分类要展开成子孙 ID
        List<Long> categoryIds = null;
        if (query.categoryId() != null) {
            categoryIds = categoryService.subtreeIds(query.categoryId());
        }

        LambdaQueryWrapper<Product> wrapper = ProductQueries.ofQuery(query, categoryIds, false);

        ProductQueries.sorted(query.normalizeSort(), wrapper);

        Page<Product> productPage =
                productMapper.selectPage(new Page<>(query.page(), query.size()), wrapper);

        return PageResult.of(productPage, ProductVO::of);
    }

    /**
     * 后台商品详情：不看上下架状态，运营要能预览草稿。
     *
     * <p>这里直接 {@code this.detail(...)} 调用，<b>不会走缓存</b>
     * （Spring 缓存代理拦截不到本类自调用）。有意为之：运营看到的必须是最新数据。
     */
    @Override
    public ProductDetailVO adminDetail(Long productId) {
        // 复用前台的取数与拼装逻辑，但自调用绕过缓存代理，每次都会查库
        return detail(productId);
    }

    /**
     * 上架 / 下架。
     *
     * <p>用 {@code allEntries = true} 整体清空详情缓存：一次改动的连带影响
     * （列表排序、分类页、搜索页）很难穷举，精确清容易漏。宁可多清，不可漏清。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)          // 更新状态要事务
    @CacheEvict(cacheNames = PRODUCT_DETAIL, allEntries = true)
    public ProductVO changeStatus(Long productId, boolean onSale) {
        // 查商品：不存在抛 12001
        Product product = getProduct(productId);

        // 按入参选状态枚举：true → ON_SALE，false → OFF_SHELF
        product.setStatus(onSale ? ProductStatus.ON_SALE : ProductStatus.OFF_SHELF);

        productMapper.updateById(product);

        log.info("商品状态变更 id={} status={}", productId, product.getStatus());

        return ProductVO.of(product);
    }

    /**
     * 刷新 SPU 冗余最低价。
     *
     * <p>只统计 {@code enabled = 1} 的 SKU；一个可用 SKU 都没有时保持原价，不清零
     * （否则商品会「凭空变成 0 元」，比价格略陈旧危险得多）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = PRODUCT_DETAIL, allEntries = true)   // 价格变了，清详情缓存
    public void refreshPrice(Long productId) {
        // 查出该商品所有启用中的 SKU
        List<Sku> skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                .eq(Sku::getProductId, productId)
                .eq(Sku::isEnabled, true));

        // 取最低价：先取价格，再过滤 null，最后按 BigDecimal 比较取最小
        BigDecimal min = skus.stream()
                .map(Sku::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        // 一个可用 SKU 都没有：保持原价不动，直接返回（绝不清零）
        if (min == null) {
            return;
        }

        Product product = getProduct(productId);

        product.setPrice(min);

        productMapper.updateById(product);
    }

    /** 查商品，找不到即 12001 */
    @Override
    public Product getProduct(Long productId) {
        // 按主键查：自动追加 deleted = 0
        Product product = productMapper.selectById(productId);

        // 不存在直接抛 12001，调用方不用判空
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }

        return product;
    }
}
