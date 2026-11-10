package com.smartmall.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 全局业务配置（对应 application.yml 里的 {@code app.*}）。
 *
 * <p>用 record 而非 setter 注入：配置一经启动就不可变，从根上杜绝"运行中被改坏"。
 * 所有字段都通过 {@code ${ENV:默认值}} 提供兜底，Day08 迁到 Nacos 配置中心时，
 * 只需把 yml 内容搬过去并加 {@code @RefreshScope}，Java 代码零改动。
 */
@ConfigurationProperties(prefix = "app")
public record MallProperties(Jwt jwt, Order order, Ai ai) {

    /** 令牌相关 */
    public record Jwt(String secret, long accessTtlSeconds, long refreshTtlSeconds) {
    }

    /** 交易相关 */
    public record Order(int payTimeoutMinutes, BigDecimal freeFreightThreshold, BigDecimal freight) {
    }

    /** 智能中心相关：chat-provider=mock 时没有大模型 Key 也能演示完整链路（Day09） */
    public record Ai(String chatProvider, String embeddingProvider) {
    }
}
