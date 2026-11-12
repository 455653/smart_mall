package com.smartmall.user.vo;

/**
 * 登录 / 刷新令牌的返回体。
 *
 * <p>为什么同时给两个令牌：
 * <ul>
 *   <li>{@code accessToken} 有效期短（2 小时），每次请求都带，泄露了损失有限</li>
 *   <li>{@code refreshToken} 有效期长（7 天），只在过期时用一次，不给业务接口用</li>
 * </ul>
 * 只有单一长令牌等于"一次泄露长期有效"，这是移动端与 SPA 的通用做法。
 *
 * @param accessToken  访问令牌，放进 Authorization: Bearer xxx
 * @param refreshToken 刷新令牌，调 /api/auth/refresh 换新的一对
 * @param tokenType    固定 Bearer，前端拼请求头用
 * @param expiresIn    访问令牌剩余有效秒数，前端据此提前续期
 * @param user         当前用户信息，省一次 /api/users/me
 */
public record LoginResponse(String accessToken, String refreshToken, String tokenType,
                            long expiresIn, UserVO user) {

    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn, UserVO user) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
