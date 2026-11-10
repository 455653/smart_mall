package com.smartmall.common.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解（单体阶段的本地滑动窗口实现）。
 *
 * <p>用法：
 * <pre>
 * &#64;RateLimit(limit = 5, windowSeconds = 60, key = "login")
 * public Result&lt;LoginVO&gt; login(...) { ... }
 * </pre>
 *
 * <p>演进：Day08 后限流规则上移到网关（Sentinel / Gateway RequestRateLimiter），
 * 这份注解实现保留给内部接口或降级为兜底。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流维度前缀，默认用"类名.方法名" */
    String key() default "";

    /** 窗口内允许的最大请求数 */
    int limit() default 20;

    /** 窗口长度（秒） */
    int windowSeconds() default 1;

    /** 是否按来源 IP 区分（登录、发券等接口强烈建议开启） */
    boolean perIp() default true;
}
