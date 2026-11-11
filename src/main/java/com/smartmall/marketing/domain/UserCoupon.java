package com.smartmall.marketing.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.marketing.enums.CouponStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户已领取的优惠券。一条记录对应一张实物券，couponCode 全局唯一。
 *
 * <p>券面额、门槛等静态信息留在模板表里，这里只存「谁领了、用没用、什么时候过期」，
 * 避免模板改价后历史券信息被连带篡改。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("mkt_user_coupon")
public class UserCoupon extends BaseEntity {

    /** 来源模板 ID，逻辑外键指向 mkt_coupon_template。 */
    private Long templateId;

    /** 持有用户 ID。 */
    private Long userId;

    /** 券码，全局唯一，核销与幂等返券都靠它定位。 */
    private String couponCode;

    /** 券状态：未使用 / 已使用 / 已过期。 */
    private CouponStatus status;

    /** 领取时间。 */
    private LocalDateTime receivedTime;

    /** 核销时间，未使用时为空。 */
    private LocalDateTime usedTime;

    /** 核销的订单号，取消订单时据此返券。 */
    private String orderNo;

    /** 本张券的过期时间，下单时由模板 endTime 固化而来。 */
    private LocalDateTime expireTime;

    /**
     * 是否已失效：已过期，或未使用但已过截止时间。
     *
     * <p>已使用的券不算失效，避免返券逻辑误判。
     */
    public boolean expired() {
        return status == CouponStatus.EXPIRED
                || (status == CouponStatus.UNUSED && expireTime != null
                && !LocalDateTime.now().isBefore(expireTime));
    }

    /**
     * 能否用于该金额的订单：券未使用、未过期，且订单金额有效。
     *
     * <p>门槛金额比对需要模板信息，由调用方用 CouponTemplate.thresholdAmount 再判一次。
     */
    public boolean usable(BigDecimal amount) {
        return status == CouponStatus.UNUSED && !expired()
                && amount != null && amount.signum() > 0;
    }

    /**
     * 标记为已核销：只有未使用的券才能核销，返回 false 说明已被用掉或已过期。
     */
    public boolean markUsed(String orderNo) {
        if (status != CouponStatus.UNUSED || expired()) {
            return false;
        }
        this.status = CouponStatus.USED;
        this.usedTime = LocalDateTime.now();
        this.orderNo = orderNo;
        return true;
    }
}