package com.smartmall.common.ratelimit;

import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流切面：基于内存的滑动窗口。
 *
 * <p>为什么是滑动窗口而不是固定窗口：固定窗口在跨窗口边界时会放行 2 倍流量
 * （59 秒来 5 个、61 秒又来 5 个，看起来"没超"），滑动窗口用时间戳队列算，边界更准。
 *
 * <p>局限：只在单机进程内有效。Day07 换 Redis + Lua 做分布式限流，
 * Day08 上移到网关统一拦——这也是"单体可用、集群失效"的典型例子。
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(joinPoint, rateLimit);
        if (!tryAcquire(key, rateLimit)) {
            log.warn("触发限流 key={} limit={}/{}s", key, rateLimit.limit(), rateLimit.windowSeconds());
            throw new BizException(ResultCode.RATE_LIMITED);
        }
        return joinPoint.proceed();
    }

    private boolean tryAcquire(String key, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowMillis = rateLimit.windowSeconds() * 1000L;

        Deque<Long> window = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            // 丢掉窗口外的旧时间戳
            while (!window.isEmpty() && now - window.peekFirst() > windowMillis) {
                window.pollFirst();
            }
            if (window.size() >= rateLimit.limit()) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String prefix = rateLimit.key();
        if (prefix.isBlank()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            prefix = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        }
        if (!rateLimit.perIp()) {
            return prefix;
        }
        return prefix + ":" + clientIp();
    }

    private String clientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
