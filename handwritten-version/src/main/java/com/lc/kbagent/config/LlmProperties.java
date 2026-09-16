package com.lc.kbagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型连接配置（application.yml 中 llm.* 前缀）。
 */
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** OpenAI 兼容接口地址，默认 DeepSeek */
    private String baseUrl = "https://api.deepseek.com";

    /** API Key */
    private String apiKey = "";

    /** 模型名，DeepSeek 默认 deepseek-chat */
    private String model = "deepseek-chat";

    /** 采样温度，知识库问答建议 0.3 左右 */
    private double temperature = 0.3;

    /** 单次回复最大 token 数 */
    private int maxTokens = 2048;

    /** 请求超时（秒） */
    private int timeoutSeconds = 60;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
