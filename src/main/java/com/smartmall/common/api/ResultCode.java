package com.smartmall.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务码：与 HTTP 状态码解耦，前端只看 code（文档 23.2）。
 *
 * <p>按模块分段，便于按 {@code code / 1000} 判断归属与排查问题：
 * <ul>
 *     <li>通用 / 认证：4000-4999</li>
 *     <li>用户：11000-11999</li>
 *     <li>订单：13000-13999</li>
 *     <li>AI：15000-15999</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(2000, "成功"),

    /* ===== 通用 / 认证 4000-4999 ===== */
    PARAM_INVALID(4001, "参数校验失败"),
    UNAUTHORIZED(4010, "未登录或登录已过期"),
    RATE_LIMITED(4029, "请求过于频繁，请稍后再试"),
    FORBIDDEN(4030, "无权限访问"),
    NOT_FOUND(4040, "资源不存在"),
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试"),

    /* ===== 用户 11000-11999 ===== */
    USER_EXISTS(11001, "账号或手机号已被注册"),
    PASSWORD_ERROR(11003, "账号或密码错误"),

    /* ===== 订单 13000-13999 ===== */
    STOCK_NOT_ENOUGH(13001, "库存不足"),
    ORDER_STATUS_ILLEGAL(13002, "当前订单状态不允许该操作"),

    /* ===== AI 15000-15999 ===== */
    AI_UNAVAILABLE(15001, "智能助手暂不可用，请稍后再试或转人工"),
    CONTENT_REJECTED(15005, "内容不符合平台规范，已被拦截");

    private final int code;
    private final String message;
}
