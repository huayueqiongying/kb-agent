package com.lc.kbagent.web.dto;

/**
 * 前端聊天请求体（会话记忆由服务端 ChatMemory 维护，前端无需回传历史）。
 */
public record ChatRequest(
        String message
) {
}
