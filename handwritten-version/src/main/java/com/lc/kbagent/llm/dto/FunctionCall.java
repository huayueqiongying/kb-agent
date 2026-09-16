package com.lc.kbagent.llm.dto;

/**
 * 工具调用的具体函数名与参数（参数为 JSON 字符串，由工具自行解析）。
 */
public record FunctionCall(
        String name,
        String arguments
) {
}
