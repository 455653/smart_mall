package com.smartmall.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.order.enums.OperatorType;
import com.smartmall.order.enums.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 订单状态流水，只追加不修改。
 *
 * <p>为什么单独建表：订单主表只有一个当前状态，出问题时必须能回答
 * "谁在什么时候把它从什么状态改成了什么状态"，这也是客服判责的唯一凭据。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("oms_order_log")
public class OrderLog extends BaseEntity {

    /** 所属订单 id */
    private Long orderId;

    /** 所属订单号，冗余便于按单号追全链路 */
    private String orderNo;

    /** 变更前状态，创建订单时为 null（没有前态） */
    private OrderStatus fromStatus;

    /** 变更后状态 */
    private OrderStatus toStatus;

    /** 操作人类型：用户/管理员/系统（超时关单是 SYSTEM） */
    private OperatorType operatorType;

    /** 操作人 id，SYSTEM 时为 null */
    private Long operatorId;

    /** 备注，如"超时未支付自动关闭""用户申请退款" */
    private String remark;

    /** 是否真的发生了状态迁移，非法空转（from == to）不应写流水 */
    public boolean statusChanged() {
        return fromStatus != toStatus;
    }

    /** 是否为系统自动触发，审计时据此和人工操作区分 */
    public boolean bySystem() {
        return operatorType == OperatorType.SYSTEM;
    }
}
