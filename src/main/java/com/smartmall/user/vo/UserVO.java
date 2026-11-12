package com.smartmall.user.vo;

import com.smartmall.user.domain.UserAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户对外视图。
 *
 * <p>为什么必须有这一层：实体里有 {@code password}、{@code tokenVersion} 这类字段，
 * 直接把实体返回前端就是安全事故。VO 是唯一对外契约，加字段要显式改这里。
 *
 * <p>手机号统一脱敏（139****0004）：即使是"查自己"，截图分享、日志打印、
 * 浏览器插件都可能把它带走。
 *
 * @param id               用户 ID
 * @param username         登录名
 * @param nickname         昵称
 * @param phone            脱敏后的手机号
 * @param avatar           头像
 * @param role             角色名
 * @param status           账号状态
 * @param points           积分
 * @param level            会员等级
 * @param totalConsumption 累计消费
 * @param lastLoginTime    最近登录时间
 * @param createTime       注册时间
 */
public record UserVO(Long id, String username, String nickname, String phone, String avatar,
                     String role, String status, int points, int level,
                     BigDecimal totalConsumption, LocalDateTime lastLoginTime,
                     LocalDateTime createTime) {

    public static UserVO of(UserAccount user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                mask(user.getPhone()),
                user.getAvatar(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getStatus() == null ? null : user.getStatus().name(),
                user.getPoints(),
                user.getLevel(),
                user.getTotalConsumption(),
                user.getLastLoginTime(),
                user.getCreateTime());
    }

    /** 13800000001 → 138****0001 */
    private static String mask(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
