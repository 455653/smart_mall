package com.smartmall.catalog.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("pms_brand")
public class Brand extends BaseEntity {

    /** 品牌名，唯一索引约束，避免同一品牌重复建档造成筛选结果分裂 */
    private String name;

    /** 品牌 Logo 地址，品牌墙与商品详情页展示，可为空 */
    private String logo;

    /** 一句话品牌介绍，用于品牌专区文案 */
    private String description;

    /** 排序权重，值越小越靠前 */
    private int sort;

    /** 是否有 Logo，前端据此决定是否占位图 */
    public boolean hasLogo() {
        return logo != null && !logo.isBlank();
    }
}