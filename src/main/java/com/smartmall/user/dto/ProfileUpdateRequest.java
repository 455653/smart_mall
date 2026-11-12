package com.smartmall.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 修改个人资料请求。
 *
 * <p>只允许改昵称和头像：账号、手机号、角色、积分都是"有业务含义"的字段，
 * 不能由用户直接改——手机号要走验证码，角色要走后台审批。
 *
 * @param nickname 昵称
 * @param avatar   头像地址
 */
public record ProfileUpdateRequest(

        @Size(max = 30, message = "昵称最长 30 个字符")
        String nickname,

        @Size(max = 255, message = "头像地址过长")
        String avatar
) {
}
