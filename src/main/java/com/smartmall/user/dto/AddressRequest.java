package com.smartmall.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 收货地址新增 / 修改请求。
 *
 * @param receiver  收货人
 * @param phone     收货人手机号
 * @param province  省
 * @param city      市
 * @param district  区/县
 * @param detail    详细地址
 * @param isDefault 是否设为默认地址，true 时会把该用户其他地址的默认位清掉
 */
public record AddressRequest(

        @NotBlank(message = "收货人不能为空")
        @Size(max = 50, message = "收货人最长 50 个字符")
        String receiver,

        @NotBlank(message = "收货手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货手机号格式不正确")
        String phone,

        @NotBlank(message = "省份不能为空")
        String province,

        @NotBlank(message = "城市不能为空")
        String city,

        @NotBlank(message = "区县不能为空")
        String district,

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 255, message = "详细地址最长 255 个字符")
        String detail,

        Boolean isDefault
) {
}
