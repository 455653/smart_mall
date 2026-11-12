package com.smartmall.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 *
 * <p>校验规则全部写在 DTO 上，Controller 只加 {@code @Valid}——
 * 校验失败时 {@code MethodArgumentNotValidException} 会把这里的 message 拼好返回，
 * 业务代码一行判断都不用写。
 *
 * @param username 登录名：字母开头，4~20 位字母数字下划线
 * @param phone    手机号：大陆 11 位
 * @param password 密码：8~32 位且必须同时包含字母与数字
 * @param nickname 昵称，可空，空则默认"用户+手机尾号"
 */
public record RegisterRequest(

        @NotBlank(message = "账号不能为空")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{3,19}$",
                message = "账号需以字母开头，4~20 位字母、数字或下划线")
        String username,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度需为 8~32 位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$",
                message = "密码需同时包含字母与数字")
        String password,

        @Size(max = 30, message = "昵称最长 30 个字符")
        String nickname
) {
}
