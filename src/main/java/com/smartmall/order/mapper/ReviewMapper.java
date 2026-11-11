package com.smartmall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.order.domain.Review;
import org.apache.ibatis.annotations.Mapper;

/** 商品评价持久层。 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
