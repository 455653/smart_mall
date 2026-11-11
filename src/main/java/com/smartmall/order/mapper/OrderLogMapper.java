package com.smartmall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.order.domain.OrderLog;
import org.apache.ibatis.annotations.Mapper;

/** 订单状态流水持久层。 */
@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLog> {
}
