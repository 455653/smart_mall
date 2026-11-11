package com.smartmall.order.enums;



import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;

import java.util.*;

/**
 * 订单状态机（文档 13.3 图 11-4 / 17.13）。
 *
 * <pre>
 * 待付款 → 待发货 → 待收货 → 已完成
 *   ↓         ↓        ↓
 * 已取消   售后中    售后中 → 已退款
 *   ↑（超时）
 * 已关闭
 * </pre>
 */
public enum OrderStatus {

    WAIT_PAY(1, "待付款"),
    WAIT_SHIP(2, "待发货"),
    WAIT_RECEIVE(3, "待收货"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消"),
    CLOSED(6, "已关闭"),
    REFUNDING(7, "售后中"),
    REFUNDED(8, "已退款");

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = buildTransitions();

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    private static Map<OrderStatus, Set<OrderStatus>> buildTransitions() {
        var map = new EnumMap<OrderStatus, Set<OrderStatus>>(OrderStatus.class);
        map.put(WAIT_PAY, EnumSet.of(WAIT_SHIP, CANCELLED, CLOSED));
        map.put(WAIT_SHIP, EnumSet.of(WAIT_RECEIVE, REFUNDING));
        map.put(WAIT_RECEIVE, EnumSet.of(COMPLETED, REFUNDING));
        map.put(COMPLETED, EnumSet.of(REFUNDING));
        map.put(REFUNDING, EnumSet.of(REFUNDED, WAIT_SHIP, WAIT_RECEIVE, COMPLETED));
        return Collections.unmodifiableMap(map);
    }

    public boolean canTransitTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public void requireTransitTo(OrderStatus target) {
        if (!canTransitTo(target)) {
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL,
                    "订单状态不允许从「%s」变更为「%s」".formatted(label, target.label));
        }
    }

    public static OrderStatus ofCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.ORDER_STATUS_ILLEGAL, "未知订单状态: " + code));
    }

    /** 是否已终结（不可再流转）。 */
    public boolean finished() {
        return this == CANCELLED || this == CLOSED || this == REFUNDED;
    }
}
