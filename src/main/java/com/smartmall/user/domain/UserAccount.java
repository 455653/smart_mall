package com.smartmall.user.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.common.security.Role;
import com.smartmall.user.enums.AccountStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("user_account")      // 表名映射；唯一约束写在建表 SQL 里
public class UserAccount extends BaseEntity {

    private String username;

    /** BCrypt 加盐哈希，禁止明文 */
    private String password;

    private String phone;

    /** 昵称，注册时不填则默认"用户+手机尾号" */
    private String nickname;

    /** 头像地址，可空 */
    private String avatar;

    private Role role = Role.CUSTOMER;            // 枚举默认按 name 存为 "CUSTOMER"

    private AccountStatus status = AccountStatus.ENABLED;

    private int points = 0;

    private int level = 1;

    /** 累计消费：会员等级与 AI 推荐画像的输入 */
    private BigDecimal totalConsumption = BigDecimal.ZERO;

    /** 令牌版本号：登出/改密后自增，使旧令牌立即失效 */
    private int tokenVersion = 0;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    public boolean enabled() {
        return status == AccountStatus.ENABLED;
    }
}