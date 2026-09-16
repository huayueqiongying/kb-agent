package com.lc.kbagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 循环配置（application.yml 中 agent.* 前缀）。
 */
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** 单轮对话最大工具调用循环次数，防止模型反复调用工具导致死循环 */
    private int maxIterations = 5;

    /** 携带给模型的历史消息条数 */
    private int historySize = 10;

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }
}
