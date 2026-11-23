package com.smartmall.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.ai.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/** AI 会话消息持久层。 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
