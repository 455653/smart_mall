package com.smartmall.user.service;

import com.smartmall.user.dto.AddressRequest;
import com.smartmall.user.vo.AddressVO;

import java.util.List;

/** 收货地址服务。所有方法都带 {@code userId}，收口越权：只能操作自己的地址。 */
public interface AddressService {

    /** 当前用户的地址列表，默认地址排在最前。 */
    List<AddressVO> list(Long userId);

    /** 查单条地址，不是本人的直接抛 40400（不暴露"存在但不属于你"）。 */
    AddressVO get(Long userId, Long addressId);

    /** 新增地址；{@code isDefault=true} 时会把该用户其他地址的默认位清掉。 */
    AddressVO create(Long userId, AddressRequest request);

    /** 修改地址；同样的默认位处理规则。 */
    AddressVO update(Long userId, Long addressId, AddressRequest request);

    /** 删除地址（逻辑删除）。 */
    void delete(Long userId, Long addressId);

    /** 把指定地址设为默认，其余地址的默认位清零。 */
    AddressVO setDefault(Long userId, Long addressId);
}
