package com.smartmall.catalog.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@TableName("pms_sku")           // 索引 idx_sku_product 写在建表 SQL
public class Sku extends BaseEntity {

    @TableField("product_id")
    private Long productId;

    private String spec;              // 例如「8GB+128GB 曜石黑」

    private BigDecimal price;

    private int stock = 0;            // 并发扣减对象

    private String image;

    private boolean enabled = true;

    public boolean hasStock() {
        return enabled && !isRemoved() && stock > 0;
    }

    /** 库存是否够指定数量：加购时先做一次快速校验，真正的闸门在扣减 SQL */
    public boolean hasStock(int num) {
        return enabled && !isRemoved() && num > 0 && stock >= num;
    }
}