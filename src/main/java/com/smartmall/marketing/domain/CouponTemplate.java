package com.smartmall.marketing.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.marketing.enums.CouponTemplateStatus;
import com.smartmall.marketing.enums.CouponType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板。表前缀 mkt_ 表示 Day08 之后归属 marketing-service（mall_marketing 库）。
 *
 * <p>issued/received 两个计数分开存：total_count 是发行总量，
 * received_count 是已领数量，领券时用条件 UPDATE 判断不超发。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("mkt_coupon_template")
public class CouponTemplate extends BaseEntity {
    /** 券名称，展示给用户看，如「满 200 减 30」。 */
    private String name;

    /** 券类型：满减券走 discountAmount，折扣券走 discountRate。
     *   满减券
     *   折扣券   enum 枚舉
     * */
    private CouponType type;

    /** 使用门槛金额：订单商品总额达到该值才可用，0 表示无门槛。 */
    private BigDecimal thresholdAmount;

    /** 满减金额，type = FULL_REDUCTION 时生效。 */
    private BigDecimal discountAmount;

    /** 折扣率，如 0.85 表示 85 折，type = DISCOUNT 时生效。 */
    private BigDecimal discountRate;

    /** 发行总量，与 receivedCount 配合控制超发。 */
    private int totalCount;

    /** 已领数量，领券成功时 +1。 */
    private int receivedCount;

    /** 每人限领张数。 */
    private int perLimit;

    /** 可领取 / 可使用的开始时间。 */
    private LocalDateTime startTime;

    /** 可领取 / 可使用的结束时间。 */
    private LocalDateTime endTime;

    /** 模板状态：生效中 / 已暂停 / 已结束，只有生效中才允许领券。 */
    private CouponTemplateStatus status;

    /** 当前是否还能领：生效中、尚未领完、且系统时间落在有效期内。 */
    public boolean receivable() {
        return status == CouponTemplateStatus.ACTIVE
                && receivedCount < totalCount
                && inPeriod(LocalDateTime.now());
    }

    /** 给定时间点是否落在券的有效期内。 */
    public boolean inPeriod(LocalDateTime time) {
        return time != null && startTime != null && endTime != null
                && !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    /** 剩余可领张数，已领完返回 0，不会出现负数。 */
    public int remaining() {
        return Math.max(totalCount - receivedCount, 0);
    }
}
