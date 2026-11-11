package com.smartmall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.order.domain.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/** 订单明细持久层。 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
