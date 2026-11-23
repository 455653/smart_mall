package com.smartmall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车视图：条目 + 结算汇总。
 *
 * <p>汇总只统计<b>勾选且可购买</b>的条目。失效商品（下架/售罄）即便勾着也不算钱，
 * 否则用户点了「去结算」会看到一个付不了的金额。
 *
 * @param items           全部条目（含失效的，前端要展示「已失效」区）
 * @param totalQuantity   所有条目数量之和，购物车角标用
 * @param checkedQuantity 勾选且可买的数量之和
 * @param checkedAmount   勾选且可买的金额之和，结算页直接用
 * @param allChecked      是否全部勾选，控制全选框状态
 */
public record CartVO(List<CartItemVO> items, int totalQuantity, int checkedQuantity,
                     BigDecimal checkedAmount, boolean allChecked) {
}
