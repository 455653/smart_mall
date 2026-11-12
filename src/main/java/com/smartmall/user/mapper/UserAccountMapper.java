package com.smartmall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.user.domain.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户账号持久层。 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 按账号查用户，账号既可以是登录名也可以是手机号。
     *
     * <p>登录时这一步必须一条 SQL 说完：先判断"像不像手机号"再决定查哪一列，
     * 会把一次查询拆成两次，还多一个分支。SQL 见 resources/mapper/user/UserAccountMapper.xml。
     */
    UserAccount selectByAccount(@Param("account") String account);

    /** 记录本次登录的时间与 IP，返回 0 表示账号不存在或已逻辑删除。 */
    int updateLastLogin(@Param("id") Long id, @Param("ip") String ip);

    /** 令牌版本自增：登出 / 改密后旧令牌立即失效，不用维护黑名单。 */
    int bumpTokenVersion(@Param("id") Long id);
}
