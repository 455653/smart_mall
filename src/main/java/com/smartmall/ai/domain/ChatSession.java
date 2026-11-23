package com.smartmall.ai.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.ai.enums.SessionStatus;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 会话。一次「打开客服窗口」对应一条记录，消息挂在会话下。
 *
 * <p>sessionId 全局唯一，前端本地缓存与断线重连都靠它。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ai_chat_session")
public class ChatSession extends BaseEntity {

    /** 会话编号，全局唯一，前端本地缓存与断线重连都靠它。 */
    private String sessionId;

    /** 所属用户 ID。 */
    private Long userId;

    /** 会话标题，一般取首条用户提问的前若干字，用于会话列表展示。 */
    private String title;

    /** 会话状态：AI 接待 / 转人工 / 已关闭。 */
    private SessionStatus status;

    /** 消息总数，便于会话列表展示未读角标。 */
    private int messageCount;

    /** 最后活跃时间，用于会话排序与超时关闭。 */
    private LocalDateTime lastActiveTime;

    /** 会话是否仍在服务中：AI 接待或人工接待都不算结束。 */
    public boolean open() {
        return status == SessionStatus.AI || status == SessionStatus.HUMAN;
    }

    /** 会话是否已关闭，关闭后不再接受新消息。 */
    public boolean closed() {
        return status == SessionStatus.CLOSED;
    }

    /** 转人工：已关闭的会话不接收转接，返回 false 由调用方提示重新发起。 */
    public boolean transferToHuman() {
        if (status == SessionStatus.CLOSED) {
            return false;
        }
        this.status = SessionStatus.HUMAN;
        return true;
    }
}
