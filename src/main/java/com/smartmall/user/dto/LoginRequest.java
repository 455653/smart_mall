package com.smartmall.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 *
 * <p>{@code account} 既可以是登录名也可以是手机号：手机号格式（1 开头 11 位）
 * 走 phone 列查，否则走 username 查。这样用户不用记自己当初是用哪个注册的。
 *
 * <p>密码这里只校验非空，<b>不校验强度和格式</b>——登录时校验格式毫无意义，
 * 只会把"账号不存在"和"密码不符合规则"区分开，帮攻击者缩小范围。
 *
 * @param account  登录名或手机号
 * @param password 明文密码，服务端用 BCrypt 比对
 */
public record LoginRequest(

        @NotBlank(message = "账号不能为空")
        String account,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
