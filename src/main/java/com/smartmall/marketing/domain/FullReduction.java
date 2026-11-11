package com.smartmall.marketing.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.marketing.enums.CouponTemplateStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("mkt_full_reduction")
public class FullReduction extends BaseEntity {

    /** 规则名称，如「双 11 满 300 减 50」。 */
    private String name;

    /** 门槛金额（满），订单商品总额达到该值才触发。 */
    private BigDecimal thresholdAmount;

    /** 减免金额（减）。 */
    private BigDecimal reduceAmount;

    /** 活动开始时间。 */
    private LocalDateTime startTime;

    /** 活动结束时间。 */
    private LocalDateTime endTime;

    /** 规则状态，复用券模板的枚举：生效中 / 已暂停 / 已结束。 */
    private CouponTemplateStatus status;

    /** 给定时间点是否在活动期内。 */
    public boolean inPeriod(LocalDateTime time) {
        return time != null && startTime != null && endTime != null
                && !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    /** 该笔金额能否享受本规则：生效中、在活动期内、且达到门槛。 */
    public boolean applicable(BigDecimal amount) {
        return status == CouponTemplateStatus.ACTIVE
                && amount != null && thresholdAmount != null
                && amount.compareTo(thresholdAmount) >= 0
                && inPeriod(LocalDateTime.now());
    }

    /**
     * 计算本规则能减多少：不满足条件返回 0，满足则返回减免金额。
     *
     * <p>不做「减免后为负」的兜底，结算侧仍需按实付下限再夹一次。
     */
    public BigDecimal reduceFor(BigDecimal amount) {
        return applicable(amount) && reduceAmount != null ? reduceAmount : BigDecimal.ZERO;
    }


}
