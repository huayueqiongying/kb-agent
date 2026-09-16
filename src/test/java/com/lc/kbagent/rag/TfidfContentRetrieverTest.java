package com.lc.kbagent.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TfidfContentRetrieverTest {

    private final TfidfContentRetriever retriever = new TfidfContentRetriever(new TfidfIndex(new KnowledgeBase(List.of(
            new KnowledgeBase.Chunk("1", "员工手册.md", "年假规定：入职满一年可享受5天带薪年假"),
            new KnowledgeBase.Chunk("2", "产品FAQ.md", "知识库问答基础版5000元每年"),
            new KnowledgeBase.Chunk("3", "公司简介.md", "公司总部位于北京，成立于2015年")
    ))));

    @Test
    void 检索返回Content且带来源元数据() {
        List<Content> contents = retriever.retrieve(Query.from("年假 带薪假期 怎么休"));
        assertFalse(contents.isEmpty());
        // 相关度最高的应是年假文档
        assertTrue(contents.get(0).textSegment().text().contains("年假"));
        // 元数据携带来源文档名
        assertTrue(contents.get(0).textSegment().metadata().getString("doc").contains("员工手册"));
    }

    @Test
    void 无关查询返回空列表() {
        List<Content> contents = retriever.retrieve(Query.from("量子纠缠"));
        assertEquals(0, contents.size());
    }
}
