package com.smartmall.ai.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.ai.enums.DocStatus;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库文档。原文入库后异步切片 + 向量化，状态从 PENDING 走向 READY / FAILED。
 *
 * <p>content 保留全文（TEXT），切片结果落到 ai_knowledge_chunk，
 * 重建索引时直接按 doc_id 重跑，不必让运营重新上传。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ai_knowledge_doc")
public class KnowledgeDoc extends BaseEntity {

    /** 文档标题，检索结果引用时展示给用户。 */
    private String title;

    /** 业务分类，如「售后政策」「商品参数」，可用于限定检索范围。 */
    private String category;

    /** 文档原文，向量化前不做破坏性清洗。 */
    private String content;

    /** 向量化状态：待向量化 / 就绪 / 失败。 */
    private DocStatus status;

    /** 切片数量，READY 后回填，便于核对切片是否完整。 */
    private int chunkCount;

    /** 失败原因，仅 FAILED 时有值，方便运营定位是切片还是 embedding 出错。 */
    private String errorMessage;

    /** 文档完成入库（切片 + 向量化）的时间点。 */
    private LocalDateTime ingestedAt;

    /** 是否已完成向量化，可被检索。 */
    public boolean ready() {
        return status == DocStatus.READY;
    }

    /** 是否向量化失败，需要运营重试。 */
    public boolean failed() {
        return status == DocStatus.FAILED;
    }

    /** 标记向量化完成并回填切片数。 */
    public void markReady(int chunkCount) {
        this.status = DocStatus.READY;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
    }
}
