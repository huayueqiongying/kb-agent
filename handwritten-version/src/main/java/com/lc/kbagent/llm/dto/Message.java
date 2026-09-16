package com.lc.kbagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 对话消息。角色分为 system / user / assistant / tool 四种：
 * <ul>
 *   <li>system：系统提示词；</li>
 *   <li>user：用户输入；</li>
 *   <li>assistant：模型回复，可能携带 tool_calls 表示要调用工具；</li>
 *   <li>tool：工具执行结果，需带 tool_call_id 与 assistant 的调用对应。</li>
 * </ul>
 */
public record Message(
        @JsonProperty("role") String role,
        @JsonProperty("content") String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId,
        @JsonProperty("name") String name
) {

    public static Message system(String content) {
        return new Message("system", content, null, null, null);
    }

    public static Message user(String content) {
        return new Message("user", content, null, null, null);
    }

    public static Message assistant(String content) {
        return new Message("assistant", content, null, null, null);
    }

    public static Message assistantWithToolCalls(String content, List<ToolCall> toolCalls) {
        return new Message("assistant", content, toolCalls, null, null);
    }

    public static Message tool(String toolCallId, String name, String content) {
        return new Message("tool", content, null, toolCallId, name);
    }
}
