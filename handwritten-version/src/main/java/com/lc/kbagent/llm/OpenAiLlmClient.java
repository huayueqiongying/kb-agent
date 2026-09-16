package com.lc.kbagent.llm;

import com.lc.kbagent.agent.AgentException;
import com.lc.kbagent.config.LlmProperties;
import com.lc.kbagent.llm.dto.ChatRequest;
import com.lc.kbagent.llm.dto.ChatResponse;
import com.lc.kbagent.llm.dto.Message;
import com.lc.kbagent.llm.dto.ToolDef;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI 兼容协议的大模型客户端实现（未依赖任何大模型 SDK）。
 *
 * <p>通过配置 base-url 即可在 DeepSeek / 通义千问 / 智谱 / 豆包 等各家模型之间切换，
 * 因为它们都实现了 OpenAI Chat Completions 协议。
 */
@Component
public class OpenAiLlmClient implements LlmClient {

    private final LlmProperties properties;
    private final RestClient restClient;

    public OpenAiLlmClient(LlmProperties properties) {
        this.properties = properties;

        // 使用 JDK 内置 HttpClient，设置读写超时
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ChatResponse chat(List<Message> messages, List<ToolDef> tools) {
        checkApiKey();

        ChatRequest request = new ChatRequest(
                properties.getModel(),
                messages,
                tools,
                properties.getTemperature(),
                properties.getMaxTokens(),
                false // 当前为一次性返回，未开启流式
        );

        try {
            ChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AgentException("大模型返回为空，请检查模型名配置是否正确");
            }
            return response;
        } catch (AgentException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            // 4xx 一般是 Key 错误 / 余额不足 / 模型名错误，把响应体透出便于排查
            throw new AgentException("调用大模型失败(HTTP " + e.getStatusCode().value() + ")："
                    + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new AgentException("调用大模型失败：" + e.getMessage(), e);
        }
    }

    private void checkApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AgentException(
                    "未配置大模型 API Key：请在 src/main/resources/application.yml 中填写 llm.api-key"
                            + "（可到 DeepSeek 开放平台 platform.deepseek.com 申请，新用户有免费额度）");
        }
    }
}
