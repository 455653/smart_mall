package com.smartmall.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.config.MallProperties;
import com.smartmall.common.exception.BizException;
import org.springframework.stereotype.Component;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 零依赖 JWT 实现（HS256）。
 *
 * <p>为什么不用 jjwt：JWT 本质就是"Base64 头.载荷.签名"，自己写 80 行能看清每个字节，
 * 比引入一个库再加一堆配置更适合教学；生产环境换 jjwt / nimbus 也是同样的三段结构。
 *
 * <p>安全要点（答辩会问）：
 * <ul>
 *   <li>载荷是 Base64 而非加密，<b>不能放密码等敏感信息</b></li>
 *   <li>签名校验用 {@link MessageDigest#isEqual} 做定长比较，防时序攻击</li>
 *   <li>必须校验 {@code exp}，否则令牌永不过期</li>
 *   <li>访问令牌与刷新令牌用 {@code typ} 声明区分：拿访问令牌去换新令牌会被拒，
 *       否则短令牌等于能无限续期，短有效期就白设了</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /** 令牌类型，写进载荷 typ */
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private static final String HEADER_B64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secretKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtUtil(MallProperties properties) {
        this.secretKey = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.accessTtlSeconds = properties.jwt().accessTtlSeconds();
        this.refreshTtlSeconds = properties.jwt().refreshTtlSeconds();
    }

    /**
     * 签发访问令牌（短有效期，每次请求都带）。
     *
     * @param userId       用户 ID
     * @param username     用户名
     * @param role         角色枚举，按 name() 写进载荷
     * @param tokenVersion 令牌版本号，登出/改密后自增，使旧令牌失效
     */
    public String createAccessToken(Long userId, String username, Role role, int tokenVersion) {
        return createToken(TYPE_ACCESS, userId, username, role, tokenVersion, accessTtlSeconds);
    }

    /**
     * 签发刷新令牌（长有效期，只在换令牌时用一次）。
     */
    public String createRefreshToken(Long userId, String username, Role role, int tokenVersion) {
        return createToken(TYPE_REFRESH, userId, username, role, tokenVersion, refreshTtlSeconds);
    }

    /** 访问令牌有效秒数，登录响应里的 expiresIn 用它 */
    public long accessTtlSeconds() {
        return accessTtlSeconds;
    }

    /**
     * 校验并解析访问令牌。签名不对、格式不对、已过期、类型不对都抛 {@link BizException}。
     */
    public LoginUser parse(String token) {
        return parse(token, TYPE_ACCESS);
    }

    /**
     * 校验并解析刷新令牌，类型必须是 refresh。
     */
    public LoginUser parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private String createToken(String type, Long userId, String username, Role role,
                               int tokenVersion, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("username", username);
        payload.put("role", role == null ? null : role.name());
        payload.put("tokenVersion", tokenVersion);
        payload.put("typ", type);
        payload.put("iat", now);
        payload.put("exp", now + ttlSeconds);

        String body;
        try {
            body = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 载荷序列化失败", e);
        }
        String signingInput = HEADER_B64 + "." + body;
        return signingInput + "." + sign(signingInput);
    }

    private LoginUser parse(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = sign(signingInput).getBytes(StandardCharsets.UTF_8);
        byte[] actual = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }

        Object expValue = payload.get("exp");
        long exp = expValue instanceof Number number ? number.longValue() : 0L;
        if (Instant.now().getEpochSecond() > exp) {
            throw new BizException(ResultCode.TOKEN_INVALID, "登录已过期，请重新登录");
        }

        if (!expectedType.equals(String.valueOf(payload.get("typ")))) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }

        Object versionValue = payload.get("tokenVersion");
        int tokenVersion = versionValue instanceof Number number ? number.intValue() : 0;

        return new LoginUser(
                Long.valueOf(String.valueOf(payload.get("sub"))),
                String.valueOf(payload.getOrDefault("username", "")),
                String.valueOf(payload.getOrDefault("role", "CUSTOMER")),
                tokenVersion);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey, HMAC_SHA256));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }
}
