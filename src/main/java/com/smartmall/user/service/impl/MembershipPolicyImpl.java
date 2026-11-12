package com.smartmall.user.service.impl;

import com.smartmall.user.service.MembershipPolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.NavigableMap;
import java.util.TreeMap;
@Service
public class MembershipPolicyImpl implements MembershipPolicy {

    // 有序   TreeMap()
        private static final NavigableMap<BigDecimal,Integer> LEVELS =   new TreeMap<>();

        // 静态初始加载块
        static {
            LEVELS.put(new BigDecimal("0"),1);

            LEVELS.put(new BigDecimal("1000"),2);

            LEVELS.put(new BigDecimal("5000"),3);

            LEVELS.put(new BigDecimal("20000"),4);

            LEVELS.put(new BigDecimal("100000"),5);

        }




    @Override
    public int leveOf(BigDecimal totalConsumption) {

            // 不充钱  0
        BigDecimal total =  totalConsumption == null ? BigDecimal.ZERO : totalConsumption;

        // 最小消费1块钱
        if (total.signum()<0) {
            return 1;
        }

        // 1200  向下去 1及 防止    空指针异常  -> NPE
        var enrty = LEVELS.floorEntry(total);

        return enrty != null ? enrty.getValue(): 1;
    }

    @Override
    public int pointOf(BigDecimal payAmount) {

        //   空置 0分  小数  99.99   给它 99分
        return payAmount == null ? 0 : payAmount.intValue();
    }
}
