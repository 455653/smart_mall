package com.smartmall.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.pay.domain.Payment;
import org.apache.ibatis.annotations.Mapper;

/** 支付单持久层。 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
