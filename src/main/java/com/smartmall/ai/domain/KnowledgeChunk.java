package com.smartmall.ai.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 知识库片段：文档按长度切分后的最小检索单元。
 *
 * <p>vector 以逗号分隔的浮点数存储，检索时全量载入内存算余弦相似度，
 * 数据量大后再换向量库。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ai_knowledge_chunk")
public class KnowledgeChunk extends BaseEntity {

    /** 所属文档 ID，逻辑外键指向 ai_knowledge_doc。 */
    private Long docId;

    /** 片段在文档内的序号，从 0 开始，用于召回后按原文顺序拼接。 */
    private int chunkIndex;

    /** 片段原文，直接作为提示词上下文喂给模型。 */
    private String content;

    /** 向量，逗号分隔的浮点数字符串，未向量化时为空。 */
    private String vector;

    /** 生成向量使用的 embedding 模型标识。 */
    private String embeddingModel;

    /** 向量维度，检索时用来跳过维度不一致的脏数据。 */
    private int dimension;

    /** 是否已生成向量，没有向量的片段不参与召回。 */
    public boolean hasEmbedding() {
        return vector != null && !vector.isBlank();
    }

    /** 是否属于指定文档，重建索引时用来过滤。 */
    public boolean belongsTo(Long docId) {
        return docId != null && docId.equals(this.docId);
    }

    /** 片段内容是否为空，空片段应跳过向量化以省调用次数。 */
    public boolean emptyContent() {
        return content == null || content.isBlank();
    }
}
