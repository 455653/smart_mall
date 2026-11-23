package com.smartmall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 修改购物车条目：改数量、改勾选状态，两个字段都可单独传。
 *
 * <p>用包装类型 {@code Integer / Boolean} 而不是基本类型，是为了区分
 * 「没传」（null，不动）与「传了 0/false」——基本类型没传会自动变 0，
 * 用户只想改勾选会被顺手把数量清成 0。
 *
 * @param quantity 新数量，null 表示不改
 * @param checked  是否勾选，null 表示不改
 */
public record UpdateCartItemRequest(

        @Min(value = 1, message = "购买数量至少 1 件")
        @Max(value = 99, message = "单个规格最多 99 件")
        Integer quantity,

        Boolean checked
) {
}
