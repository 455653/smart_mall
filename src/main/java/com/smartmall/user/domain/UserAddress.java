package com.smartmall.user.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 收货地址。一个用户可有多条，下单时拷贝成快照写进 oms_order，避免地址改动影响历史订单。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("user_address")
public class UserAddress extends BaseEntity {

    /** 归属用户，收货地址是用户私有数据，查询必须带上它防越权 */
    private Long userId;

    /** 收货人姓名，快递面单直接使用 */
    private String receiver;

    /** 收货人手机号，快递联系与短信通知使用 */
    private String phone;

    /** 省，与市、区一起构成结构化地址，便于按区域算运费 */
    private String province;

    /** 市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址，门牌号等，与省市区拼接后才是完整地址 */
    private String detail;

    /**
     * 是否默认地址，1 是 0 否。
     * 用 Integer 而非 boolean：Lombok 会为 boolean 生成 isDefault()，MyBatis-Plus 反射解析属性名会错位。
     */
    private Integer isDefault = 0;

    /** 是否默认地址，业务代码读这里，避免各处写魔法值 1 */
    public boolean isDefaultAddress() {
        return Integer.valueOf(1).equals(isDefault);
    }

    /** 是否属于指定用户，越权校验用 */
    public boolean belongsTo(Long userId) {
        return userId != null && userId.equals(this.userId);
    }

    /** 拼出完整地址，订单快照与运费计算共用 */
    public String fullAddress() {
        return province + city + district + detail;
    }

}
