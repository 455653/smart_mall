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

    /** 回补库存（取消订单、退款时用），返回 0 表示规格不存在或已停用。 */
    int restoreStock(@Param("skuId") Long skuId, @Param("num") int num);
}
