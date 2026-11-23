package com.smartmall.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建订单请求。
 *
 * <p>不传商品清单：下什么由<b>购物车勾选项</b>决定。让前端传清单等于把
 * 「买多少、买哪个」的决定权交给客户端，容易和购物车不一致，也给了刷单的空间。
 *
 * @param addressId 收货地址 ID，服务端据此查地址并生成快照
 * @param requestNo 幂等键，前端生成；同一单重复提交只会产生一张订单
 */
public record CreateOrderRequest(

        @NotNull(message = "收货地址不能为空")
        Long addressId,

        @NotBlank(message = "请求号不能为空")
        @Size(max = 40, message = "请求号最长 40 个字符")
        String requestNo
) {
}
