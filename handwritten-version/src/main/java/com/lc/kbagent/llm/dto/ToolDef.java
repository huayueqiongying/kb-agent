package com.lc.kbagent.llm.dto;

/**
 * 对外暴露给大模型的工具定义（JSON Schema 形式）。
 */
public record ToolDef(
        String type,
        FunctionDef function
) {
}
