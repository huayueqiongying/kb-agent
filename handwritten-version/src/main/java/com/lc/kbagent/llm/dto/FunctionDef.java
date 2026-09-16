package com.lc.kbagent.llm.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 函数定义：名称、描述与参数 JSON Schema。
 */
public record FunctionDef(
        String name,
        String description,
        JsonNode parameters
) {
}
