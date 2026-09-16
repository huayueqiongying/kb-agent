package com.lc.kbagent.agent;

import com.lc.kbagent.config.AgentProperties;
import com.lc.kbagent.config.LlmProperties;
import com.lc.kbagent.rag.TfidfContentRetriever;
import com.lc.kbagent.tools.AgentTools;
import com.lc.kbagent.tools.ToolCallTracker;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;

/**
 * Agent 门面：将「大模型 + 工具 + RAG 检索 + 会话记忆」组装为可对话的助手。
 *
 * <p>LangChain4j 的 AiServices 内部自动处理函数调用循环，
 * 本类只负责装配与结果封装。
 */
@Service
public class AgentService {

    private final KbAssistant assistant;
    private final ToolCallTracker tracker;
    private final LlmProperties llmProperties;

    public AgentService(ChatModel chatLanguageModel,
                        TfidfContentRetriever contentRetriever,
                        AgentTools agentTools,
                        ToolCallTracker tracker,
                        AgentProperties agentProperties,
                        LlmProperties llmProperties) {
        this.tracker = tracker;
        this.llmProperties = llmProperties;

        this.assistant = AiServices.builder(KbAssistant.class)
                .chatModel(chatLanguageModel)
                .contentRetriever(contentRetriever)   // RAG：知识库检索增强，降低幻觉
                .tools(agentTools)                     // Function Calling：4 个工具
                .chatMemory(MessageWindowChatMemory.builder()
                        .maxMessages(agentProperties.getMaxMessages())
                        .build())
                .build();
    }

    /**
     * 发起一次对话，返回最终回答与本次调用的工具列表。
     */
    public AgentReply chat(String userMessage) {
        if (llmProperties.getApiKey() == null || llmProperties.getApiKey().isBlank()) {
            throw new AgentException("未配置大模型 API Key：请在 src/main/resources/application.yml 中填写 llm.api-key"
                    + "（可到 DeepSeek 开放平台 platform.deepseek.com 申请，新用户有免费额度）");
        }
        tracker.start();
        try {
            String reply = assistant.chat(userMessage);
            return new AgentReply(reply, tracker.tools(), "ok");
        } finally {
            tracker.clear();
        }
    }
}
