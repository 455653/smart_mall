package com.smartmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能商城启动类。
 *
 * <p>四个 @Enable* 就是本项目的"时间维度"能力：缓存（快）、异步（不阻塞）、调度（定时）、配置（可切）。
 */
@SpringBootApplication          // 组合注解：配置类 + 自动装配 + 组件扫描
@ConfigurationPropertiesScan    // 扫描 @ConfigurationProperties 的 record（本项目所有配置类）
@EnableCaching                  // 开启 @Cacheable（商品详情、分类树）
@EnableAsync                    // 开启 @Async（知识库向量化、审计日志）
@EnableScheduling               // 开启 @Scheduled（超时关单、优惠券过期）
public class SmartMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMallApplication.class, args);
    }
}
