package com.smartmall.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.catalog.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/** 商品 SPU 持久层。 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 评价后刷新商品评分与评价数，SQL 见 resources/mapper/catalog/ProductMapper.xml。 */
    int updateRating(@Param("id") Long id,
                     @Param("rating") BigDecimal rating,
                     @Param("reviewCount") int reviewCount);
}
