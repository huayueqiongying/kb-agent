package com.lc.kbagent.web.dto;

import java.util.List;

/**
 * 前端聊天请求体。
 */
public record ChatRequest(
        String message,
        List<HistoryMessage> history
) {
}
