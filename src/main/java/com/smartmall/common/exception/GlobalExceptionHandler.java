package com.smartmall.common.exception;

import com.smartmall.common.api.Result;
import com.smartmall.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：把异常挡在 Controller 之外（文档 17.14）。
 *
 * <p>@RestControllerAdvice 对所有 Controller 生效，且能捕获拦截器 preHandle 里抛的异常，
 * 所以后续的 AccessInterceptor 直接 throw new BizException(UNAUTHORIZED) 即可。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：预期内的失败，打 warn，不打堆栈 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：把 @NotBlank 等注解的 message 拼起来返回 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        var message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? error.getField() : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.PARAM_INVALID, message);
    }

    /** 访问不存在的接口 / 静态资源：统一返回 4040 资源不存在 */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public Result<Void> handleNotFound(Exception e) {
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /** 兜底：系统异常绝不把堆栈暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);          // 完整堆栈只进日志
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}
