package com.smartmall.common.exception;

import com.smartmall.common.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常：预期内的失败，由 GlobalExceptionHandler 转成统一返回（文档 17.14）。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);            // 允许覆盖默认文案，例如「库存不足，当前仅剩 3 件」
        this.code = resultCode.getCode();
    }
}
