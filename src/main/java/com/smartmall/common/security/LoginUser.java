package com.smartmall.common.security;

/**
 * 当前登录用户（从令牌里解析出来的最小身份信息）。
 *
 * @param userId   用户 ID（令牌 sub）
 * @param username 用户名
 * @param role     角色：USER / ADMIN
 */
public record LoginUser(Long userId, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
