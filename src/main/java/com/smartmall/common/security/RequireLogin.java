package com.smartmall.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记“必须登录才能访问”。
 *
 * <p>空标记注解：不关心具体是谁，只要求有身份。可以标在方法上，也可以标在类上
 * （方法级优先，方法没写才看类）。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {
}
