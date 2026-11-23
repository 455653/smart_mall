package com.smartmall.user.service.impl;

import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;
import com.smartmall.user.domain.UserAccount;
import com.smartmall.user.dto.ProfileUpdateRequest;
import com.smartmall.user.mapper.UserAccountMapper;
import com.smartmall.user.service.UserService;
import com.smartmall.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// log日志  3种引入方式  公司的方式
@Slf4j
@Service
@RequiredArgsConstructor  // 为 final 字段生成构造器，Spring 用它注入依赖
public class UserServiceImpl implements UserService {



    // 引入mapper   构造方法注入     spring 依赖循环容易故障
    private  final UserAccountMapper userAccountMapper;

    @Override
    public UserVO me(Long userId) {

        // 用id 查数据库
        UserAccount user = getUser(userId);
        // 返回vo对象  封装User
        return UserVO.of(user);
    }
    @Override
    public UserAccount getUser(Long userId) {
        // 查询数据 按id查询user
        UserAccount user = userAccountMapper.selectById(userId);
        // 判断是否为空
        if (user == null) {
            // 抛出自定义异常  如果为空
            throw  new BizException(ResultCode.USER_NOT_FOUND);
        }
        // 返回controller
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)   // 先查后改必须原子，避免并发下改到过期数据
    public UserVO updateProfile(Long userId, ProfileUpdateRequest request) {

        // 先查完整实体：既是存在性校验（不存在抛 11001），也拿到当前值做"未传则保留"。
        // 不能 new UserAccount() 打补丁后 updateById——实体里 role/status/points/level/
        // totalConsumption/tokenVersion 都带初始值，非 null 会一并进 SET，
        // 改个昵称就会把角色降级、积分清零、令牌版本重置。
        UserAccount user = getUser(userId);

        // 昵称：非 null 且非空白才更新，空串视为"不修改"，避免把昵称清空
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname().trim());
        }

        // 头像：只判 null（允许传空串表示清空头像），非 null 才覆盖
        if (request.avatar() != null) {
            user.setAvatar(request.avatar().trim());
        }

        userAccountMapper.updateById(user);

        log.info("用户资料已更新 userId={}", userId);

        // 回读最新数据，返回给前端
        return UserVO.of(getUser(userId));
    }


}
