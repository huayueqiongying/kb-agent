package com.lc.kbagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI 兼容 Chat Completions 响应体。
 */
public record ChatResponse(
        List<Choice> choices,
        Usage usage
) {

    public record Choice(
            Message message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    public record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {
    }
}
