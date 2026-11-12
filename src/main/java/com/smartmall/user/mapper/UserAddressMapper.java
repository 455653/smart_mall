package com.smartmall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.user.domain.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 收货地址持久层。 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {

    /** 清掉该用户所有地址的默认标记，SQL 见 resources/mapper/user/UserAddressMapper.xml。 */
    int clearDefault(@Param("userId") Long userId);

    /** 把指定地址置为默认；带 user_id 条件，防止越权改到别人的地址。 */
    int markDefault(@Param("userId") Long userId, @Param("id") Long id);
}
