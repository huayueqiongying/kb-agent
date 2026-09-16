package com.lc.kbagent.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 把手写实现 TF-IDF 检索封装为 LangChain4j 的 {@link ContentRetriever}，
 * 从而接入 AiServices 的 RAG 链路（检索 → 增强 → 生成）。
 *
 * <p>升级为向量库召回（Milvus/ES 等）时只需替换本实现类，
 * 上层 AgentService 与工具无需任何改动。
 */
@Component
public class TfidfContentRetriever implements ContentRetriever {

    private static final int TOP_K = 3;

    private final TfidfIndex index;

    public TfidfContentRetriever(TfidfIndex index) {
        this.index = index;
    }

    @Override
    public List<Content> retrieve(Query query) {
        return index.search(query.text(), TOP_K).stream()
                .map(hit -> Content.from(TextSegment.from(
                        hit.chunk().content(),
                        Metadata.from(Map.of(
                                "doc", hit.chunk().docName(),
                                "score", String.format("%.2f", hit.score()))))))
                .toList();
    }
}
