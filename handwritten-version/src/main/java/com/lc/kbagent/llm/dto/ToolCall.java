package com.lc.kbagent.llm.dto;

/**
 * 模型要求调用某个工具的指令。
 */
public record ToolCall(
        String id,
        String type,
        FunctionCall function
) {
}
