package com.smartmall.catalog.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.catalog.enums.ProductStatus;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@TableName("pms_product")       // 索引写在建表 SQL：idx_product_category_status / idx_product_brand
public class Product extends BaseEntity {

    @TableField("category_id")  // 下划线命名才需要显式映射，驼峰字段可省略
    private Long categoryId;

    @TableField("brand_id")
    private Long brandId;

    private String title;

    private String subtitle;

    @TableField("main_image")
    private String mainImage;

    /** 图集，逗号分隔（小数据量下比 JSON 列更省事） */
    private String images;

    private String detail;

    /** AI 生成的卖点，必须运营审核后才展示（文档 22.4） */
    @TableField("ai_selling_points")
    private String aiSellingPoints;

    /** 冗余所有 SKU 的最低价，列表页排序/筛选直接用它，避免每页联表聚合 */
    private BigDecimal price = BigDecimal.ZERO;

    private ProductStatus status = ProductStatus.OFF_SHELF;

    private int sales = 0;

    private BigDecimal rating = new BigDecimal("5.00");

    @TableField("review_count")
    private int reviewCount = 0;

    public boolean onSale() {
        return status == ProductStatus.ON_SALE && !isRemoved();
    }
}