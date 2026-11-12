package com.smartmall.user.controller;

import com.smartmall.common.api.Result;
import com.smartmall.common.security.UserContext;
import com.smartmall.user.dto.AddressRequest;
import com.smartmall.user.service.AddressService;
import com.smartmall.user.vo.AddressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
// @RequireLogin
@RequestMapping("/api/users/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // 地址列表
    @GetMapping
    public Result<List<AddressVO>> list() {
        val userId = UserContext.requireUserId();
        return Result.ok(addressService.list(userId));
    }

    // 地址详情
    @GetMapping("/{id}")
    public Result<AddressVO> get(@PathVariable Long id) {
        val userId = UserContext.requireUserId();
        return Result.ok(addressService.get(userId, id));
    }

    // 新增地址
    @PostMapping
    public Result<AddressVO> create(@Valid @RequestBody AddressRequest request) {
        val userId = UserContext.requireUserId();
        return Result.ok(addressService.create(userId, request));
    }

    // 修改地址
    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        val userId = UserContext.requireUserId();
        return Result.ok(addressService.update(userId, id, request));
    }

    // 删除地址
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        val userId = UserContext.requireUserId();
        addressService.delete(userId, id);
        return Result.ok();
    }

    // 设为默认地址
    @PutMapping("/{id}/default")
    public Result<AddressVO> setDefault(@PathVariable Long id) {
        val userId = UserContext.requireUserId();
        return Result.ok(addressService.setDefault(userId, id));
    }

}
