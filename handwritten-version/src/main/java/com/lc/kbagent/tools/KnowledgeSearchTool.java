package com.lc.kbagent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lc.kbagent.rag.TfidfIndex;
import org.springframework.stereotype.Component;

/**
 * 工具：知识库检索（RAG 的检索环节）。
 *
 * <p>基于 TF-IDF + 余弦相似度在预加载的知识库文档片段中召回最相关的若干条，
 * 返回给大模型作为作答依据，从而减少模型幻觉。
 */
@Component
public class KnowledgeSearchTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TOP_K = 3;

    private final TfidfIndex index;

    public KnowledgeSearchTool(TfidfIndex index) {
        this.index = index;
    }

    @Override
    public String name() {
        return "searchKnowledgeBase";
    }

    @Override
    public String description() {
        return "在企业知识库中检索与用户问题最相关的文档片段。"
                + "回答关于公司制度、员工手册、产品功能、价格、售后、联系方式等知识库问题时，必须先调用本工具，"
                + "再基于检索结果回答。参数 query 为检索关键词或问题核心内容。";
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        ObjectNode query = JsonNodeFactory.instance.objectNode();
        query.put("type", "string");
        query.put("description", "检索关键词或用户问题的核心内容，例如：年假、报销流程、产品价格");
        properties.set("query", query);
        params.set("properties", properties);
        params.set("required", JsonNodeFactory.instance.arrayNode().add("query"));
        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        String query = parseQuery(argumentsJson);
        if (query == null || query.isBlank()) {
            return "参数解析失败：请提供 query 参数，例如 {\"query\": \"年假\"}";
        }

        var hits = index.search(query.trim(), TOP_K);
        if (hits.isEmpty()) {
            return "知识库检索结果：未检索到与「" + query + "」相关的内容。";
        }

        StringBuilder sb = new StringBuilder("知识库检索结果（按相关度从高到低）：\n");
        for (int i = 0; i < hits.size(); i++) {
            TfidfIndex.Hit hit = hits.get(i);
            sb.append("【").append(i + 1).append("】来源文档：").append(hit.chunk().docName())
                    .append("｜相关度：").append(String.format("%.2f", hit.score())).append("\n");
            sb.append(hit.chunk().content()).append("\n");
        }
        return sb.toString();
    }

    private String parseQuery(String argumentsJson) {
        try {
            JsonNode node = MAPPER.readTree(argumentsJson);
            JsonNode query = node.path("query");
            return query.isTextual() ? query.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
