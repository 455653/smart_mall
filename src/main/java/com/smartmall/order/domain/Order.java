package com.smartmall.order.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.order.enums.OrderStatus;
import com.smartmall.pay.enums.PayChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("oms_order")     // 唯一键 uk_order_no / uk_order_request_no、索引 idx_order_user_status_time 写在建表 SQL
public class Order extends BaseEntity {

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;      // 商品总额

    @TableField("discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;   // 优惠合计

    @TableField("coupon_amount")
    private BigDecimal couponAmount = BigDecimal.ZERO;     // 其中券的抵扣

    @TableField("freight_amount")
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @TableField("pay_amount")
    private BigDecimal payAmount = BigDecimal.ZERO;        // 实付

    private OrderStatus status = OrderStatus.WAIT_PAY;      // 枚举按 name 存 "WAIT_PAY"

    @TableField("pay_type")
    private PayChannel payType;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("expire_time")                              // 超时关单的判定依据
    private LocalDateTime expireTime;

    @TableField("close_time")
    private LocalDateTime closeTime;

    /** 收货信息快照（JSON），后续改地址不影响历史订单 */
    @TableField("receiver_snapshot")
    private String receiverSnapshot;

    /** 幂等键：同一请求号只会成功一次 */
    @TableField("request_no")
    private String requestNo;

    @TableField("coupon_id")
    private Long couponId;

    @TableField("seckill_id")
    private Long seckillId;

    @TableField("item_count")
    private int itemCount = 0;

    @TableField("logistics_company")
    private String logisticsCompany;

    @TableField("tracking_no")
    private String trackingNo;

    public boolean ownedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public boolean payable() {
        return status == OrderStatus.WAIT_PAY && LocalDateTime.now().isBefore(expireTime);
    }
}