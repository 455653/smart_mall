package com.smartmall.user.controller;

import com.smartmall.common.api.Result;
import com.smartmall.common.ratelimit.RateLimit;
import com.smartmall.common.security.UserContext;
import com.smartmall.user.dto.LoginRequest;
import com.smartmall.user.dto.RefreshTokenRequest;
import com.smartmall.user.dto.RegisterRequest;
import com.smartmall.user.service.AuthService;
import com.smartmall.user.vo.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// @RequireLogin
@RequestMapping("/api/users")
@RequiredArgsConstructor  // 构造器注入  静态方法注入
public class AuthController {


    private final AuthService authService;

    // 注册
    @PostMapping("/register")
    // 限流  注册的人数多  同一ip 每60秒  最多注册10次
    // perIp=true 按ip 不是限制人数
    @RateLimit(key = "auth:register", limit = 10, windowSeconds = 60, perIp = true)
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {

        // 调用服务 注册
        val register = authService.register(registerRequest);

        return Result.ok(register);

    }

    @PostMapping("/login")
    // 限流：同一 IP 每 60 秒最多 10 次登录尝试
    @RateLimit(key = "auth:login", limit = 10, windowSeconds = 60, perIp = true)
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                       HttpServletRequest request) {

        // 取客户端 IP，写进 user_account.last_login_ip，异地登录排查要用
        val login = authService.login(loginRequest, clientIp(request));

        return Result.ok(login);
    }

    @PostMapping("/refresh")
    // 刷新令牌：同一 IP 每 60 秒最多 20 次
    @RateLimit(key = "auth:refresh", limit = 20, windowSeconds = 60, perIp = true)
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {

        val refreshed = authService.refresh(refreshTokenRequest);

        return Result.ok(refreshed);
    }

    @PostMapping("/logout")
    // 登出：令牌版本自增，已签发的旧令牌全部失效
    public Result<Void> logout() {

        authService.logout(UserContext.requireUserId());

        return Result.ok();
    }

    /** 取真实客户端 IP：优先 X-Forwarded-For 的第一段，没有则退回直连地址。 */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
