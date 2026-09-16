package com.lc.kbagent.web.dto;

/**
 * 前端传入的单条历史消息。
 */
public record HistoryMessage(
        String role,
        String content
) {
}
