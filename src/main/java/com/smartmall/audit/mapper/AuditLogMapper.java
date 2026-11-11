package com.smartmall.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.audit.domain.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作审计日志持久层。 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
