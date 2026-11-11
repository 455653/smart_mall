package com.smartmall.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.catalog.domain.Category;
import org.apache.ibatis.annotations.Mapper;

/** 商品分类持久层。 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
