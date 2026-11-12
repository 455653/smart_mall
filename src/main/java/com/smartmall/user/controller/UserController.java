package com.smartmall.user.controller;

import com.smartmall.common.api.Result;
import com.smartmall.common.security.UserContext;
import com.smartmall.user.dto.ProfileUpdateRequest;
import com.smartmall.user.service.UserService;
import com.smartmall.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// @RequireLogin
@RequestMapping("/api/users")
@RequiredArgsConstructor  // 构造器注入  静态方法注入      spring 循环依赖
public class UserController {

    private  final UserService userService;


    // 查看当前登录用户资料。
    @GetMapping("/me")
    public Result<UserVO> me() {
        // 查看 在令牌的id
        val l = UserContext.requireUserId();
        //  按id  查最新的资料   转vo   17
        UserVO response = userService.me(l);
        return Result.ok(response);
     }

    // 修改当前登录用户资料（昵称 / 头像）。
    @PutMapping("/me")
    public Result<UserVO> updateMe(@Valid @RequestBody ProfileUpdateRequest request) {
        // 身份从令牌里取，不接受前端传 userId，避免越权改别人
        val userId = UserContext.requireUserId();

        UserVO response = userService.updateProfile(userId, request);
        return Result.ok(response);
    }


}
