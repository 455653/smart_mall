package com.smartmall.common.persistence;
import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：所有表共用主键、审计字段、逻辑删除。
 *
 * <p>四个字段的含义与约定：
 * <ul>
 *   <li>{@code id} —— 雪花 ID（{@link IdType#ASSIGN_ID}），分布式拆分后各服务各自生成也不会撞号</li>
 *   <li>{@code createTime / updateTime} —— 由 {@link AuditMetaObjectHandler} 自动填充，业务代码不手写</li>
 *   <li>{@code deleted} —— 逻辑删除，0 正常 1 已删；MP 会自动把 delete 语句改写成 update</li>
 * </ul>
 */
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;


    public boolean isRemoved() {
        return deleted != null && deleted == 1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }


}
