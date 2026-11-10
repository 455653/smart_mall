package com.smartmall.common.security;

import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;

/**
 * 当前请求的用户身份（ThreadLocal 传递）。
 *
 * <p>演进接缝：单体阶段由过滤器解析令牌后写入；Day08 拆服务后，改由网关校验令牌、
 * 把用户身份写进 {@code X-User-Id} 请求头，服务端拦截器再还原到这里的同一个 ThreadLocal。
 * <b>业务代码读身份的方式一个字都不用改</b> —— 这就是"演进预留"。
 *
 * <p>使用纪律：请求结束必须 {@link #clear()}，否则线程池复用时会串号（最典型的安全事故）。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 可能为 null（未登录访问放行接口时） */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 可能为 null，便于"未登录也可访问"的场景 */
    public static Long userIdOrNull() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    /** 必须已登录，否则抛 40100 */
    public static Long requireUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user.userId();
    }

    /** 必须是管理员，否则抛 40300 */
    public static Long requireAdminId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (!user.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return user.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
