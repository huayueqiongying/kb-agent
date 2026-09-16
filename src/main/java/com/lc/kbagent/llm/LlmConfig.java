package com.lc.kbagent.llm;

import com.lc.kbagent.config.LlmProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 大模型 Bean 配置：基于 LangChain4j 的 OpenAI 兼容客户端。
 *
 * <p>LangChain4j 与各家模型的对接统一在模型模块中完成，
 * 通过 baseUrl 即可切换：DeepSeek / 通义(兼容模式) / 智谱 / 豆包 等。
 */
@Configuration
public class LlmConfig {

    @Bean
    public ChatModel chatLanguageModel(LlmProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }
}
