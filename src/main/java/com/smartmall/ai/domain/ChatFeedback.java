package com.smartmall.ai.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 回答满意度反馈。用户对单条 AI 消息点赞 / 点踩，可附文字说明。
 *
 * <p>rating 为 GOOD（满意）/ BAD（不满意）两档，保持轻量。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ai_chat_feedback")
public class ChatFeedback extends BaseEntity {

    /** 评分：GOOD 满意，BAD 不满意。 */
    private String rating;

    /** 被评价的 AI 消息 ID，逻辑外键指向 ai_chat_message。 */
    private Long messageId;

    /** 所属会话 ID，便于按会话聚合满意度。 */
    private String sessionId;

    /** 评价人 ID。 */
    private Long userId;

    /** 文字反馈，点踩时通常用来收集具体问题。 */
    private String reason;

    /** 是否为正面评价。 */
    public boolean satisfied() {
        return "GOOD".equalsIgnoreCase(rating);
    }

    /** 是否为负面评价。 */
    public boolean unsatisfied() {
        return "BAD".equalsIgnoreCase(rating);
    }

    /** 是否附带文字说明。 */
    public boolean hasComment() {
        return reason != null && !reason.isBlank();
    }
}
