package com.smartmall.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.order.enums.AfterSaleStatus;
import com.smartmall.order.enums.AfterSaleType;
import com.smartmall.order.enums.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后单。表前缀 oms_，随交易域归 order-service。
 *
 * <p>设计要点：origin_order_status 记录申请前的订单状态，驳回时才能精确回滚，
 * 否则"待收货"申请售后被驳回后会错误地变成"待发货"。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("oms_after_sale")
public class AfterSale  extends BaseEntity {

    /** 售后单号，唯一，对外展示用 */
    private String afterSaleNo;

    /** 关联订单 id */
    private Long orderId;

    /** 关联订单号，退款时按单号反查支付单 */
    private String orderNo;

    /** 申请用户，列表与越权校验依据 */
    private Long userId;

    /** 售后类型：仅退款 / 退货退款，决定是否需要回寄物流 */
    private AfterSaleType type;

    /** 申请原因，用户填写，客服判责依据 */
    private String reason;

    /** 售后状态，库中存枚举 name() */
    private AfterSaleStatus status;

    /** 申请退款金额，不得超过订单实付，审核时校验 */
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /** 申请前的订单状态，驳回时据此回滚，避免状态被覆盖丢失 */
    private OrderStatus originOrderStatus;

    /** 申请时间，用于"7 天无理由"等时效判断 */
    private LocalDateTime applyTime;

    /** 处理时间，客服通过/驳回的时刻，用于考核处理时效 */
    private LocalDateTime handleTime;

    /** 处理备注，驳回时必须写清理由 */
    private String handleRemark;

    /** 归属校验：防止 A 用户改 id 查看/撤销 B 用户的售后单 */
    public boolean ownedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    /** 是否还在待处理队列中，客服工作台按此筛选 */
    public boolean waitingHandle() {
        return status == AfterSaleStatus.APPLIED;
    }

    /** 是否必须回寄商品，仅"退货退款"需要，前端据此决定是否展示填单号入口 */
    public boolean returnRequired() {
        return type == AfterSaleType.RETURN_REFUND;
    }
}

