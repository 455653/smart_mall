package com.smartmall.user.service;

import com.smartmall.user.dto.LoginRequest;
import com.smartmall.user.dto.RefreshTokenRequest;
import com.smartmall.user.dto.RegisterRequest;
import com.smartmall.user.vo.LoginResponse;

public interface AuthService {

    /** 注册并直接签发一对令牌 */
    LoginResponse register(RegisterRequest request);

    /**
     * 登录。
     *
     * @param ip 来源 IP，写进 user_account.last_login_ip，异地登录排查要用
     */
    LoginResponse login(LoginRequest request, String ip);

    /** 用刷新令牌换一对新令牌 */
    LoginResponse refresh(RefreshTokenRequest request);

    /** 登出：令牌版本自增，让已签发的旧令牌全部失效 */
    void logout(Long userId);
}
