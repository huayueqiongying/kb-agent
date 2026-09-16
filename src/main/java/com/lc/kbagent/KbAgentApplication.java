package com.lc.kbagent;

import com.lc.kbagent.config.AgentProperties;
import com.lc.kbagent.config.LlmProperties;
import com.lc.kbagent.config.ToolProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 企业知识库智能问答 Agent 启动类。
 *
 * <p>核心能力：
 * 1. 基于大模型函数调用（Function Calling）的 Agent 循环，可自主决定调用哪些工具；
 * 2. 内置 4 个工具：知识库检索（TF-IDF）、当前时间、安全计算器、天气查询；
 * 3. 不依赖 LangChain4j / Spring AI 等框架，手写 OpenAI 兼容协议，可无缝切换各家大模型。
 */
@SpringBootApplication
@EnableConfigurationProperties({LlmProperties.class, AgentProperties.class, ToolProperties.class})
public class KbAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(KbAgentApplication.class, args);
    }
}
