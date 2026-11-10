package com.smartmall.common.api;

import org.slf4j.MDC;

/**
 * 统一返回体。所有 Controller（含未来的六个微服务）都返回它，前端零改造。
 *
 * <p>JSON 形如：
 * <pre>
 * { "code": 0, "message": "成功", "data": {...}, "traceId": "a1b2c3d4" }
 * </pre>
 *
 * @param code    0 表示成功，非 0 见 {@link ResultCode}
 * @param message 面向用户的提示语
 * @param data    业务数据
 * @param traceId 链路追踪 ID（日志 MDC 中的 traceId，排查问题靠它把一次请求串起来）
 */
public record Result<T>(int code, String message, T data, String traceId) {

    private static final String MDC_TRACE_ID = "traceId";

    /** 成功并返回数据 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, currentTraceId());
    }

    /** 成功无数据 */
    public static Result<Void> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null, currentTraceId());
    }

    /** 按错误码失败 */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null, currentTraceId());
    }

    /** 按错误码失败，但换一句更具体的提示 */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null, currentTraceId());
    }

    /** 自定义码失败 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, currentTraceId());
    }

    /** 是否成功，便于内部判断 */
    public boolean success() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

    private static String currentTraceId() {
        String traceId = MDC.get(MDC_TRACE_ID);
        return traceId == null ? "" : traceId;
    }
}
