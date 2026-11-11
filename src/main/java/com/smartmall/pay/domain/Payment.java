package com.smartmall.pay.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.pay.enums.PayChannel;
import com.smartmall.pay.enums.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单。表前缀 pay_，归属 pay-service（mall_pay 库）。
 *
 * <p>为什么和订单分表：一单可能多次发起支付（用户关掉又重付），
 * 但 uk_payment_order 保证一个订单号最终只有一条有效支付单，
 * 金额也独立落库，财务对账以支付单为准而不是订单。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("pay_payment")
public class Payment extends BaseEntity {


    /** 支付单号，唯一，传给第三方渠道的商户订单号 */
    private String paymentNo;

    /** 关联订单号，唯一索引；支付回调靠它反查订单 */
    private String orderNo;

    /** 付款用户，回调时校验与订单归属一致，防串单 */
    private Long userId;

    /** 支付渠道，库中存枚举 name() */
    private PayChannel channel;

    /** 支付金额，必须等于订单 pay_amount，不一致直接拒单 */
    private BigDecimal amount;

    /** 支付状态，库中存枚举 name() */
    private PaymentStatus status;

    /** 第三方交易号，退款与对账的凭证，未回调前为 null */
    private String tradeNo;

    /** 回调时间，与订单 pay_time 交叉核对可发现漏单 */
    private LocalDateTime callbackTime;

    /** 归属校验：防止 A 用户查询/操作 B 用户的支付单 */
    public boolean ownedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    /** 是否已支付成功，回调处理必须幂等，先判此值再更新 */
    public boolean paid() {
        return status == PaymentStatus.PAID;
    }

    /** 是否还能发起支付，只有未支付且在有效期内才允许 */
    public boolean payable() {
        return status == PaymentStatus.PENDING;
    }


}
