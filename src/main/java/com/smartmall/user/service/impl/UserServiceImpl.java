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
    public UserVO updateProfile(Long userId, ProfileUpdateRequest request) {

        // 先确认用户存在，不存在直接 11001
        getUser(userId);

        // 只挑"确实传了值"的字段更新：MP 默认忽略 null 字段，
        // 空串（前端清空昵称/头像）也按"不改"处理，避免把昵称写成空。
        UserAccount patch = new UserAccount();
        patch.setId(userId);
        if (request.nickname() != null && !request.nickname().isBlank()) {
            patch.setNickname(request.nickname().trim());
        }
        if (request.avatar() != null && !request.avatar().isBlank()) {
            patch.setAvatar(request.avatar().trim());
        }
        userAccountMapper.updateById(patch);

        log.info("用户资料已更新 userId={}", userId);

        // 回读最新数据，返回给前端
        return UserVO.of(getUser(userId));
    }


}
