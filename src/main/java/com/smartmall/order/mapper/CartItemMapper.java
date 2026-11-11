package com.smartmall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.order.domain.CartItem;
import org.apache.ibatis.annotations.Mapper;

/** 购物车持久层。 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
