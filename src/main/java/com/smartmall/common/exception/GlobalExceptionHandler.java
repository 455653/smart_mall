package com.smartmall.common.exception;

import com.smartmall.common.api.Result;
import com.smartmall.common.api.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把各类异常统一翻译成 {@link Result}，前端永远只解析一种结构。
 *
 * <p>三档处理策略：
 * <ol>
 *   <li><b>业务异常</b> {@link BizException} —— 预期内，按错误码返回，日志 warn，不打堆栈</li>
 *   <li><b>参数异常</b> —— 400，把第一个校验错误提示给用户（提示语要能直接看懂）</li>
 *   <li><b>未知异常</b> —— 500，日志 error 打全栈，对外只说"系统繁忙"，绝不泄露堆栈</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：库存不足、订单状态非法等 */
    @ExceptionHandler(com.smartmall.common.exception.BizException.class)
    public Result<Void> handleBiz(com.smartmall.common.exception.BizException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.BAD_REQUEST, msg.isBlank() ? ResultCode.BAD_REQUEST.getMessage() : msg);
    }

    /** 表单/查询参数绑定失败 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.BAD_REQUEST, msg.isBlank() ? ResultCode.BAD_REQUEST.getMessage() : msg);
    }

    /** 方法参数上的 @Validated 约束失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** 必填参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "缺少必要参数：" + e.getParameterName());
    }

    /** 参数类型不匹配，如路径里的 id 传了 abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "参数类型不正确：" + e.getName());
    }

    /**
     * 访问了不存在的路径 / 静态资源。
     *
     * <p>必须单独拦：Spring 6.1 起这种情况抛的是 {@code NoResourceFoundException}，
     * 如果只留兜底的 {@code Exception} 处理器，访问错地址会返回 500，把前端和运维都带偏。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在：/" + e.getResourcePath());
    }

    /** 兜底：绝不把堆栈返回给前端 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未捕获异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }

    private String fieldMessage(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }
}
