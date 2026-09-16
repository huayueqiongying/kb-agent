package com.lc.kbagent.tools;

import com.lc.kbagent.config.ToolProperties;
import com.lc.kbagent.rag.KnowledgeBase;
import com.lc.kbagent.rag.TfidfIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 工具单测：直接构造 AgentTools（不依赖 Spring 容器、不联网）。
 */
class AgentToolsTest {

    private AgentTools tools;
    private ToolCallTracker tracker;

    @BeforeEach
    void setUp() {
        TfidfIndex index = new TfidfIndex(new KnowledgeBase(List.of(
                new KnowledgeBase.Chunk("1", "员工手册.md", "年假规定：入职满一年可享受5天带薪年假，满3年7天"),
                new KnowledgeBase.Chunk("2", "产品FAQ.md", "知识库问答基础版5000元每年")
        )));
        ToolProperties properties = new ToolProperties();
        properties.getWeather().setMock(true);   // 天气走模拟数据，不联网
        tracker = new ToolCallTracker();
        // 天气 mock 模式下不会用到 RestClient，传 null 即可
        tools = new AgentTools(index, properties, null, tracker);
    }

    @Test
    void 知识库检索返回相关片段() {
        String result = tools.searchKnowledgeBase("年假怎么休");
        assertTrue(result.contains("年假规定"));
        assertTrue(result.contains("员工手册.md"));
    }

    @Test
    void 无关检索返回未找到提示() {
        String result = tools.searchKnowledgeBase("量子力学");
        assertTrue(result.contains("未检索到"));
    }

    @Test
    void 当前时间返回北京时间格式() {
        String result = tools.getCurrentTime();
        assertTrue(result.startsWith("当前时间（北京时间）"));
        assertTrue(result.contains("年"));
    }

    @Test
    void 计算器返回格式化结果() {
        assertEquals("计算结果：(1+2)*3 = 9", tools.calculate("(1+2)*3"));
        assertTrue(tools.calculate("1/0").contains("无法解析"));
    }

    @Test
    void 天气模拟模式返回演示数据() {
        String result = tools.getWeather("北京");
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("模拟数据"));
    }

    @Test
    void 工具调用被记录() {
        tracker.start();
        tools.getCurrentTime();
        tools.calculate("1+1");
        assertEquals(List.of("getCurrentTime", "calculate"), tracker.tools());
        tracker.clear();
        assertEquals(List.of(), tracker.tools());
    }

    @Test
    void 工具列表包含四个工具() {
        List<Map<String, String>> describe = tools.describeTools();
        assertEquals(4, describe.size());
        List<String> names = describe.stream().map(m -> m.get("name")).toList();
        assertTrue(names.containsAll(List.of(
                "searchKnowledgeBase", "getCurrentTime", "calculate", "getWeather")));
    }
}
