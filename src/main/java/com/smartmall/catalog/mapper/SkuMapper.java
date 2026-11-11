package com.smartmall.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.catalog.domain.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 商品 SKU 持久层。 */
@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

    /** 条件扣减库存，返回 0 表示库存不足，SQL 见 resources/mapper/catalog/SkuMapper.xml。 */
    int deductStock(@Param("skuId") Long skuId, @Param("num") int num);
}
