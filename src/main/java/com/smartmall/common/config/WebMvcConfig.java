package com.smartmall.common.config;

import com.smartmall.common.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册鉴权拦截器。
 *
 * <p>只拦 {@code /api/**}，不放行任何业务路径的“白名单”——是否需要登录
 * 由接口上的 {@code @RequireLogin} / {@code @RequireRole} 决定，
 * 而不是在这里维护一份容易漏配的路径表。<b>注解比配置更不容易忘。</b>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }
}
