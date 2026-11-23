package com.smartmall.cart.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 全选 / 取消全选。
 *
 * @param checked true 全选，false 全部取消勾选
 */
public record CheckAllRequest(

        @NotNull(message = "checked 不能为空")
        Boolean checked
) {
}
