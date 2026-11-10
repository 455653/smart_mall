package com.smartmall.common.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/*
*  字段填充  createTime updateTime deleted
*
*  每個表都有這三個時間
*
*  把它抽取出來  工具類    不要重複寫
* */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {


    // 新增  刪除  創建
    @Override
    public void insertFill(MetaObject metaObject) {

        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    // 修改
    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

    }
}
