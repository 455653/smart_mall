package com.smartmall.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.ai.domain.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/** AI 会话持久层。 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
