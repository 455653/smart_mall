package com.smartmall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.marketing.domain.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

/** 用户优惠券持久层。 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
}
