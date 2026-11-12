package com.smartmall.common.security;

/**
 * 当前登录用户（从令牌里解析出来的最小身份信息）。
 *
 * @param userId       用户 ID（令牌 sub）
 * @param username     用户名
 * @param role         角色：CUSTOMER / SERVICE / OPERATOR / ADMIN
 * @param tokenVersion 令牌版本号，与库里 user_account.token_version 比对，
 *                     不一致说明用户已登出或改密，该令牌立即作废
 */
public record LoginUser(Long userId, String username, String role, int tokenVersion) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
