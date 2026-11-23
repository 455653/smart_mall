package com.smartmall.common.security;

import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 鉴权拦截器：读 {@link RequireLogin} / {@link RequireRole} 并裁决。
 *
 * <p>与过滤器的分工：
 * <ul>
 *   <li>{@link JwtAuthFilter} 答<b>你是谁</b>（认证），把身份放进 {@link UserContext}</li>
 *   <li>本拦截器答<b>你能不能</b>（鉴权），读注解决定放不放行</li>
 * </ul>
 *
 * <p>为什么拦截器里可以放心抛异常：它跑在 DispatcherServlet 内部，
 * 异常会被 {@code @RestControllerAdvice} 接住，转成统一的 40100 / 40300 JSON。
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 静态资源、错误页等不是 HandlerMethod，直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        var loginRequired = resolve(handlerMethod, RequireLogin.class);
        var roleRequired = resolve(handlerMethod, RequireRole.class);

        // 先认证：没登录的请求不该进入更贵的逻辑
        if ((loginRequired != null || roleRequired != null) && !UserContext.isAuthenticated()) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 再鉴权：登录了但角色不对才是 40300
        if (roleRequired != null) {
            LoginUser current = UserContext.require();
            Role[] allowed = roleRequired.value();
            boolean hit = Arrays.stream(allowed)
                    .anyMatch(role -> role.name().equalsIgnoreCase(current.role()));
            if (!hit) {
                log.warn("越权访问 uri={} user={} role={} required={}", request.getRequestURI(),
                        current.username(), current.role(), names(allowed));
                throw new BizException(ResultCode.FORBIDDEN,
                        "需要「" + names(allowed) + "」角色才能操作");
            }
        }
        return true;
    }

    /** 方法级优先、类级兜底 */
    private <A extends Annotation> A resolve(HandlerMethod handlerMethod, Class<A> type) {
        A onMethod = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), type);
        return onMethod != null
                ? onMethod
                : AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), type);
    }

    private String names(Role[] roles) {
        return Arrays.stream(roles).map(Role::name).collect(Collectors.joining("/"));
    }
}
