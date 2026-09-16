package com.lc.kbagent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TfidfIndexTest {

    private final TfidfIndex index = new TfidfIndex(new KnowledgeBase(List.of(
            new KnowledgeBase.Chunk("1", "java.md", "Java 后端开发使用 Spring Boot 框架，负责接口开发"),
            new KnowledgeBase.Chunk("2", "travel.md", "北京旅游景点推荐：故宫、长城、颐和园"),
            new KnowledgeBase.Chunk("3", "salary.md", "公司每月10日发放工资，报销到账约5-7个工作日")
    )));

    @Test
    void 能召回相关文档并按相关度排序() {
        List<TfidfIndex.Hit> hits = index.search("Spring Boot 接口开发", 3);
        assertFalse(hits.isEmpty());
        assertEquals("java.md", hits.get(0).chunk().docName());
        assertTrue(hits.get(0).score() >= hits.get(hits.size() - 1).score());
    }

    @Test
    void 中文二元组检索() {
        List<TfidfIndex.Hit> hits = index.search("工资什么时候发", 3);
        assertFalse(hits.isEmpty());
        assertEquals("salary.md", hits.get(0).chunk().docName());
    }

    @Test
    void 无关查询返回空或低相关() {
        List<TfidfIndex.Hit> hits = index.search("量子计算原理", 3);
        assertTrue(hits.isEmpty());
    }
}
