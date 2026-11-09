package com.smartmall.common.api;

/**
 * 统一返回结构：code / message / data（文档 14.4 / 17.14 / 23.2）。
 *
 * @param code    业务码，2000 表示成功
 * @param message 提示信息
 * @param data    业务数据
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static Result<Void> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 用业务码但覆盖默认文案，例如参数校验失败时拼接 @NotBlank 的 message */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean successful() {
        return code == ResultCode.SUCCESS.getCode();
    }
}
