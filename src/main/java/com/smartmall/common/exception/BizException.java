package com.smartmall.common.exception;

import com.smartmall.common.api.ResultCode;

/**
 * 业务异常。业务代码里"主动抛、全局接"，不写 try-catch 到处兜。
 *
 * <p>用法：{@code throw new BizException(ResultCode.STOCK_NOT_ENOUGH);}
 *
 * <p>刻意继承 {@link RuntimeException} 而不是受检异常：业务异常是"预期内的失败"，
 * 强制每个方法声明 throws 只会污染签名。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // 业务异常不需要完整调用栈，省掉栈采集能降低开销（高频抛出的场景有感）
        return this;
    }
}
