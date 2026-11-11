package com.smartmall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.user.domain.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/** 收货地址持久层。 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
