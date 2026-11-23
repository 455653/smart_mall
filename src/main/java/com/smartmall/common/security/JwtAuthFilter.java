package com.smartmall.common.security;

import com.smartmall.user.domain.UserAccount;
import com.smartmall.user.mapper.UserAccountMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：把请求头里的令牌变成“当前用户”。
 *
 * <p>职责只有一个 —— 回答<b>你是谁</b>。至于“你能不能访问”，交给
 * {@link AuthInterceptor}（读 {@link RequireLogin} / {@link RequireRole} 裁决）。
 *
 * <p>两类请求在这里都是放行：
 * <ul>
 *   <li>没带令牌：游客，可能访问的是公开接口</li>
 *   <li>令牌非法 / 过期：等同于游客，由拦截器按注解返回 40100</li>
 * </ul>
 * 过滤器里<b>不抛异常</b>：它跑在 DispatcherServlet 之前，抛出去走不到
 * {@code @RestControllerAdvice}，前端会收到一个 HTML 错误页。
 *
 * <p>为什么每次都要查库：用户可能被禁用、被删、登出（tokenVersion 自增），
 * 只看令牌会“幽灵登录”。代价是每请求一次主键查询——有索引，单体阶段可接受。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserAccountMapper userAccountMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } finally {
            // 必须清理：Tomcat 线程池会复用线程，不清理就会把上一个用户的身份串给下一个请求
            UserContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            return;
        }
        String token = header.substring(BEARER.length()).trim();

        LoginUser loginUser;
        try {
            // 签名、过期、typ=access 三重校验都在 parse 里做掉
            loginUser = jwtUtil.parse(token);
        } catch (RuntimeException e) {
            // 令牌非法一律当游客处理，不在这里抛异常
            return;
        }

        UserAccount user = userAccountMapper.selectById(loginUser.userId());  // 逻辑删除由 @TableLogic 自动排除
        if (user == null || !user.enabled()) {
            return;
        }
        // 令牌版本对不上说明已登出或改密，旧令牌立即作废
        if (user.getTokenVersion() != loginUser.tokenVersion()) {
            return;
        }

        // 角色取库里的最新值，而不是令牌里的旧值
        UserContext.set(new LoginUser(user.getId(), user.getUsername(),
                user.getRole() == null ? null : user.getRole().name(), user.getTokenVersion()));
    }
}
