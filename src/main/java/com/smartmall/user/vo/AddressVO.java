package com.smartmall.user.vo;

import com.smartmall.user.domain.UserAddress;

/**
 * 收货地址视图。
 *
 * <p>这里手机号<b>不脱敏</b>：地址是用户自己的私有数据，下单和改地址都需要完整号码；
 * 已经通过"归属校验 + 登录态"把越权挡在外面了。
 *
 * @param id       地址 ID
 * @param receiver 收货人
 * @param phone    收货手机号
 * @param province 省
 * @param city     市
 * @param district 区/县
 * @param detail   详细地址
 * @param isDefault 是否默认地址
 * @param fullAddress 省市区+详细地址拼好的完整串，前端直接展示
 */
public record AddressVO(Long id, String receiver, String phone, String province, String city,
                        String district, String detail, boolean isDefault, String fullAddress) {

    public static AddressVO of(UserAddress address) {
        return new AddressVO(
                address.getId(),
                address.getReceiver(),
                address.getPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetail(),
                address.isDefaultAddress(),
                address.fullAddress());
    }
}
