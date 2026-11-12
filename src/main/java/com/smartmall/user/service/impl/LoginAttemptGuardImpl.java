package com.smartmall.user.service.impl;

// 登录失败锁定接口：本类实现它


import com.smartmall.user.service.LoginAttemptGuard;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败锁定实现：连续错 5 次，锁 15 分钟。
 *
 * <p>数据结构是 {@code ConcurrentHashMap<String, Attempt>}，每个账号一个计数器。
 * 用 {@link ConcurrentHashMap#computeIfAbsent} 而不是"先 get 再 put"，
 * 是为了避免两个线程同时首次失败时创建出两个 Attempt，导致计数只记到其中一个。
 *
 * <p>为什么 {@code Attempt} 内部还要 {@code synchronized}：{@code failures++} 不是原子操作，
 * 而 {@code computeIfAbsent} 只保证"创建"这一下的原子性，不保证后续读改原子。
 * 锁在 Attempt 对象上，不同账号之间完全不竞争（锁粒度是 per-key）。
 *
 * <p>局限：计数在 JVM 内存里，重启即清零；多实例部署各算各的；
 * {@code attempts} 只在"锁定过期被访问"时清理。Day07 换 Redis 的 INCR + EXPIRE 可全解，
 * 本接口签名不用改——这正是先抽接口的价值。
 */
@Service
public class LoginAttemptGuardImpl implements LoginAttemptGuard {

    // 连续失败多少次开始锁定
    private static final int MAX_FAILURES = 5;

    // 锁定时长：15 分钟
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    // 账号 → 失败记录；key 已归一化（trim + 小写），避免 "Tom" 与 "tom " 被当成两个账号
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * 距离解锁还有多少秒，0 表示未锁定。
     *
     * <p>这里顺带做惰性清理：锁定已过期时把该账号从 Map 删掉，
     * 否则 Map 会随着"试错过的账号"无限增长（撞库攻击下会被撑爆内存）。
     */
    @Override
    public long lockedSeconds(String account) {
        // 取该账号的失败记录；从没失败过则为 null
        Attempt attempt = attempts.get(key(account));

        // 没有记录说明从未失败，直接返回 0（未锁定）
        if (attempt == null) {
            return 0;
        }

        // 锁住单个 Attempt 对象做读改：锁粒度是 per-key，不同账号互不阻塞
        synchronized (attempt) {
            // 计算剩余锁定时间（毫秒）= 截止时间 - 当前时间
            long remaining = attempt.lockedUntil - System.currentTimeMillis();

            // 剩余时间 <= 0 有两种可能：从未锁定（lockedUntil=0）或锁定已到期
            if (remaining <= 0) {
                // 只有"曾经锁过且已过期"才需要从 Map 移除，避免误删正在累计的失败计数
                if (attempt.lockedUntil > 0) {
                    // 移除过期记录：既清理内存，也让下次失败从 0 重新累计
                    attempts.remove(key(account));
                }
                // 未锁定
                return 0;
            }

            // 还有 remaining 毫秒，换算成秒返回（向下取整）
            return remaining / 1000;
        }
    }

    /**
     * 记一次失败，达到阈值开始锁定并清零计数（锁定期满重新累计）。
     *
     * <p>为什么达到阈值后要把 {@code failures} 清零：锁定到期后用户还有 5 次机会。
     * 如果保留 5，用户解锁后第一次输错就又被锁 15 分钟，等于永久封号。
     */
    @Override
    public void onFailure(String account) {
        // 拿到（或创建）该账号的失败记录：computeIfAbsent 保证并发下只创建一次
        Attempt attempt = attempts.computeIfAbsent(key(account), k -> new Attempt());

        // 锁住这个记录再自增：failures++ 不是原子操作
        synchronized (attempt) {
            // 失败次数 +1
            attempt.failures++;

            // 达到阈值（5 次）就开始锁定
            if (attempt.failures >= MAX_FAILURES) {
                // 锁定截止时间 = 当前时间 + 15 分钟
                attempt.lockedUntil = System.currentTimeMillis() + LOCK_DURATION.toMillis();

                // 计数清零：解锁后重新给 5 次机会，否则解锁即再锁
                attempt.failures = 0;
            }
        }
    }

    /**
     * 登录成功清零。
     *
     * <p>直接 {@code remove} 而不是把 failures 置 0：整个对象都不要了，Map 也顺带瘦身。
     */
    @Override
    public void onSuccess(String account) {
        // 移除该账号的失败与锁定记录（下次失败会重新创建）
        attempts.remove(key(account));
    }

    /**
     * 账号归一化。
     *
     * <p>为什么转小写：用户名本身可能大小写敏感，但失败计数不该被 "Admin"/"admin"
     * 绕过——攻击者换个大小写就重置计数，等于没做。
     */
    private String key(String account) {
        // null 归一成空串防止 NPE；否则去首尾空格并转小写
        return account == null ? "" : account.trim().toLowerCase();
    }

    /** 单个账号的失败记录；私有静态内部类，不对外暴露 */
    private static final class Attempt {
        // 当前连续失败次数（达到 MAX_FAILURES 时锁定并清零）
        private int failures;

        // 锁定截止时间戳（毫秒），0 表示未锁定
        private long lockedUntil;
    }
}
