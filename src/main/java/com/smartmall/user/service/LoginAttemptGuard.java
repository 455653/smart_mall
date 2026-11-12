package com.smartmall.user.service;

/**
 * 登录失败锁定接口。
 *
 * <p>为什么有了限流还要做它：限流挡"单位时间请求数"，但攻击者可以慢速撞库
 * （每分钟 3 次，永远打不满窗口）。失败计数是<b>按账号</b>累加的，慢也没用。
 *
 * <p>两条纪律：账号不存在也要计失败（防枚举）；登录成功立刻清零（不拖累正常用户）。
 *
 * <p>实现类见 {@code com.liys.user.service.impl.LoginAttemptGuardImpl}。
 * 当前用 JVM 内存计数，多实例各算各的；换 Redis 的 {@code INCR + EXPIRE} 时接口不变。
 *
 * @see com.smartmall.user.service.impl.LoginAttemptGuardImpl
 */
public interface LoginAttemptGuard {

    /**
     * 查询距离解锁还剩多少秒。
     *
     * @param account 登录账号（用户名或手机号）
     * @return 剩余秒数；返回 0 表示未被锁定
     */
    // 查锁定：返回剩余秒数，0 表示可正常登录
    long lockedSeconds(String account);

    /**
     * 记录一次登录失败；累计达到阈值后开始锁定。
     *
     * @param account 登录账号
     */
    // 记失败：计数 +1，达到阈值写入锁定截止时间
    void onFailure(String account);

    /**
     * 记录一次登录成功，清除该账号的失败计数与锁定状态。
     *
     * @param account 登录账号
     */
    // 记成功：直接移除该账号的失败记录
    void onSuccess(String account);
}
