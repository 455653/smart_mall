package com.smartmall.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
*  1 2 3 4 分頁
*
* 用插件來實現
*
*
* */
@Configuration
public class MybatisPlusConfig {


    // 對象  springboot當中  分頁 的
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {

        var interceptor = new MybatisPlusInterceptor();

        // 防全表更新/删除：update 或 delete 没带 where 条件时直接抛异常
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }



}
