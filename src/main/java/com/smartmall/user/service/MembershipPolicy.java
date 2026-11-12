package com.smartmall.user.service;

import java.math.BigDecimal;

// 大佬的面子问题  消费等级
public interface MembershipPolicy {

    //  按消费 计算等级会员
    int  leveOf(BigDecimal totalConsumption);

    // 算积分  截断取整数 不足1元不给
    int  pointOf(BigDecimal payAmount);


}
