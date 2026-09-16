package com.lc.kbagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenAI 兼容 Chat Completions 请求体。
 *
 * <p>发送给大模型的消息列表 + 可用工具列表（Function Calling 协议）。
 */
public record ChatRequest(
        String model,
        java.util.List<Message> messages,
        java.util.List<ToolDef> tools,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("stream") boolean stream
) {
}
