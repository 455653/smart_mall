package com.smartmall.user.service;

import com.smartmall.user.domain.UserAccount;
import com.smartmall.user.dto.ProfileUpdateRequest;
import com.smartmall.user.vo.UserVO;

public interface UserService {

    // 查询当前登录用户的资料，手机号脱敏为 {@code 138****0004}。
    UserVO me(Long userId);


    UserAccount getUser(Long userId);

    /** 修改当前登录用户的昵称 / 头像，返回更新后的资料。 */
    UserVO updateProfile(Long userId, ProfileUpdateRequest request);

}
