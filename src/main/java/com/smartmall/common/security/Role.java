package com.smartmall.common.security;

import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 系统角色（文档 13.10 角色与权限矩阵）。
 */
public enum Role {

    CUSTOMER("顾客"),
    SERVICE("客服"),
    OPERATOR("运营"),
    ADMIN("管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 解析角色，非法值返回 Optional.empty()，避免 isXXX 抛异常。 */
    public static Optional<Role> parse(String value) {
        return Optional.ofNullable(value)
                .map(v -> v.trim().toUpperCase(Locale.ROOT))
                .flatMap(v -> Arrays.stream(values())
                        .filter(role -> role.name().equals(v))
                        .findFirst());
    }

    public static Role of(String value) {
        return parse(value).orElseThrow(() -> new BizException(ResultCode.FORBIDDEN, "未知角色: " + value));
    }

    /** 运营、管理员拥有后台管理权限（运营不含账号与系统配置）。 */
    public boolean isBackOffice() {
        return this == OPERATOR || this == ADMIN;
    }
}
