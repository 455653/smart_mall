package com.smartmall.common.config;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.context.annotation.Bean;

/**
 * 密码编码器。
 *
 * <p>只引了 {@code spring-security-crypto}，没有引完整的 Spring Security 过滤器链——
 * 登录鉴权由 Day03 自己写的 JWT 过滤器完成，这里只借它的 BCrypt 实现。
 *
 * <p>为什么用 BCrypt 而不是 MD5/SHA：
 * <ul>
 *   <li>自带随机盐，同一个密码每次哈希结果都不同，彩虹表失效</li>
 *   <li>计算慢（cost=10 约 60ms），暴力破解成本高；这个"慢"正是它的安全来源</li>
 * </ul>  加密用的 密碼加盐
 */
public class SecurityConfig {


    @Bean
    public PasswordEncoder passwordEncoder(){

        return new BCryptPasswordEncoder();
    }
}
