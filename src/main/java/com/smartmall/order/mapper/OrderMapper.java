package com.smartmall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.order.domain.Order;
import org.apache.ibatis.annotations.Mapper;

/** 订单持久层。 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
