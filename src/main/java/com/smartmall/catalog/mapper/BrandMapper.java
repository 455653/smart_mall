package com.smartmall.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.catalog.domain.Brand;
import org.apache.ibatis.annotations.Mapper;

/** 品牌持久层。 */
@Mapper
public interface BrandMapper extends BaseMapper<Brand> {
}
