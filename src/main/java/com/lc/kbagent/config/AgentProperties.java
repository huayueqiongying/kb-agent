package com.lc.kbagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 配置（application.yml 中 agent.* 前缀）。
 */
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** 会话记忆最多保留的消息条数（MessageWindowChatMemory，超出后丢弃最旧消息） */
    private int maxMessages = 10;

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }
}
