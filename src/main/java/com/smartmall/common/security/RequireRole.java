package com.smartmall.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记“只允许指定角色访问”，满足其一即可。
 *
 * <p>用于后台接口，如商品管理 {@code @RequireRole({Role.OPERATOR, Role.ADMIN})}。
 * 隐含“必须登录”——没登录时先是 40100，登录了角色不对才是 40300。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许的角色集合，命中任意一个即通过 */
    Role[] value();
}
