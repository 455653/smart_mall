package com.smartmall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.user.domain.UserAccount;
import org.apache.ibatis.annotations.Mapper;

/** 用户账号持久层。 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}
