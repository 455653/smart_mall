package com.smartmall.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;
import com.smartmall.user.domain.UserAddress;
import com.smartmall.user.dto.AddressRequest;
import com.smartmall.user.mapper.UserAddressMapper;
import com.smartmall.user.service.AddressService;
import com.smartmall.user.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<AddressVO> list(Long userId) {
        List<UserAddress> addresses = userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                // 默认地址排最前，其余按创建时间倒序（新加的在前）
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime));
        return addresses.stream().map(AddressVO::of).toList();
    }

    @Override
    public AddressVO get(Long userId, Long addressId) {
        return AddressVO.of(requireOwned(userId, addressId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(Long userId, AddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        applyField(address, request);
        // true 时先清掉其他默认，再插入的这条才是唯一默认
        boolean asDefault = Boolean.TRUE.equals(request.isDefault());
        if (asDefault) {
            userAddressMapper.clearDefault(userId);
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }
        userAddressMapper.insert(address);
        log.info("新增收货地址 userId={} addressId={}", userId, address.getId());
        return AddressVO.of(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = requireOwned(userId, addressId);
        applyField(address, request);
        // isDefault 传了才动，没传保持原样
        if (Boolean.TRUE.equals(request.isDefault())) {
            userAddressMapper.clearDefault(userId);
            address.setIsDefault(1);
        } else if (Boolean.FALSE.equals(request.isDefault())) {
            address.setIsDefault(0);
        }
        userAddressMapper.updateById(address);
        log.info("修改收货地址 userId={} addressId={}", userId, addressId);
        return AddressVO.of(address);
    }

    @Override
    public void delete(Long userId, Long addressId) {
        requireOwned(userId, addressId);
        // MP 逻辑删除：改 deleted=1，历史订单的快照不受影响
        userAddressMapper.deleteById(addressId);
        log.info("删除收货地址 userId={} addressId={}", userId, addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO setDefault(Long userId, Long addressId) {
        UserAddress address = requireOwned(userId, addressId);
        // 先清后置，保证同一用户只有一条默认
        userAddressMapper.clearDefault(userId);
        userAddressMapper.markDefault(userId, addressId);
        address.setIsDefault(1);
        return AddressVO.of(address);
    }

    /** 越权防护：查出来必须是本人的，否则一律按"不存在"处理。 */
    private UserAddress requireOwned(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !address.belongsTo(userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    private void applyField(UserAddress address, AddressRequest request) {
        address.setReceiver(request.receiver());
        address.setPhone(request.phone());
        address.setProvince(request.province());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setDetail(request.detail());
    }
}
