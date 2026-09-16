package com.lc.kbagent.agent;

import com.lc.kbagent.config.AgentProperties;
import com.lc.kbagent.llm.LlmClient;
import com.lc.kbagent.llm.dto.ChatResponse;
import com.lc.kbagent.llm.dto.FunctionCall;
import com.lc.kbagent.llm.dto.Message;
import com.lc.kbagent.llm.dto.ToolCall;
import com.lc.kbagent.tools.Tool;
import com.lc.kbagent.tools.ToolRegistry;
import com.lc.kbagent.web.dto.HistoryMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 核心：工具调用（Function Calling）主循环。
 *
 * <p>执行流程（一次对话）：
 * <pre>
 * 1. 组装 system 提示词 + 历史消息 + 用户问题，连同全部工具定义发给大模型；
 * 2. 模型返回两种可能：
 *    a) 直接给出最终回答 → 结束，返回答案；
 *    b) 返回 tool_calls（想调用工具）→ 依次执行工具，把结果作为 tool 消息追加，
 *       回到第 1 步继续让模型基于结果作答（最多 maxIterations 轮）。
 * </pre>
 * 这个"思考→行动→观察"的循环正是 Agent 的核心思想（ReAct 范式的简化实现）。
 */
@Service
public class AgentService {

    private static final String SYSTEM_PROMPT = """
            你是「星辰科技」的智能知识库问答助手，负责根据企业内部知识库回答员工与客户的问题。

            回答规则：
            1. 回答与公司制度、产品、政策、流程相关的问题时，必须先调用 searchKnowledgeBase 工具检索知识库，并基于检索到的内容作答，禁止编造知识库中不存在的信息。
            2. 引用知识库内容时，请说明信息来自哪个文档（如"根据《员工手册》"）。
            3. 涉及"当前时间/日期"的问题，调用 getCurrentTime 获取准确时间；涉及数学计算的问题，调用 calculate 工具。
            4. 涉及天气的问题，调用 getWeather 工具。
            5. 如果检索不到相关信息，请如实告知"知识库中暂未找到相关内容"，并建议用户联系行政部或查看 OA 系统，不要强行编造。
            6. 回答保持简洁、友好、专业，使用中文，一般不超过 200 字。
            """;

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final AgentProperties properties;

    public AgentService(LlmClient llmClient, ToolRegistry toolRegistry, AgentProperties properties) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
    }

    public AgentReply chat(String userMessage, List<HistoryMessage> history) {
        List<Message> messages = buildMessages(userMessage, history);
        List<String> toolsUsed = new ArrayList<>();

        int iterations = 0;
        while (iterations < properties.getMaxIterations()) {
            ChatResponse response = llmClient.chat(messages, toolRegistry.toolDefs());
            Message assistantMessage = response.choices().get(0).message();

            // 把模型这条回复加入上下文（工具调用场景必须保留，否则模型会"失忆"）
            messages.add(assistantMessage);

            List<ToolCall> toolCalls = assistantMessage.toolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                // 模型直接给出了最终回答，循环结束
                return new AgentReply(assistantMessage.content(), toolsUsed, iterations + 1,
                        response.usage(), "ok");
            }

            // 模型要求调用工具：逐个执行并把结果回填到对话上下文
            for (ToolCall toolCall : toolCalls) {
                FunctionCall function = toolCall.function();
                if (function == null || function.name() == null) {
                    continue;
                }
                toolsUsed.add(function.name());
                String result = executeTool(function.name(), function.arguments());
                messages.add(Message.tool(toolCall.id(), function.name(), result));
            }
            iterations++;
        }

        // 达到最大循环轮数仍没得到最终答案：返回最后一条内容并提示
        Message last = messages.get(messages.size() - 1);
        return new AgentReply(last.content() == null ? "（未能生成回答，请重试）" : last.content(),
                toolsUsed, iterations, null, "max_iterations");
    }

    /**
     * 执行单个工具，任何异常都转为文本结果回传给模型，避免整个对话失败。
     */
    private String executeTool(String name, String argumentsJson) {
        Tool tool = toolRegistry.get(name);
        if (tool == null) {
            return "错误：未知工具「" + name + "」，请从可用工具中选择";
        }
        try {
            return tool.execute(argumentsJson);
        } catch (Exception e) {
            return "工具执行异常：" + e.getMessage();
        }
    }

    /**
     * 组装完整消息列表：system + 历史（裁剪）+ 本次提问。
     */
    private List<Message> buildMessages(String userMessage, List<HistoryMessage> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.system(SYSTEM_PROMPT));

        int kept = 0;
        if (history != null) {
            // 只保留尾部最近的 historySize 条，控制上下文长度与 token 成本
            List<HistoryMessage> tail = history.size() > properties.getHistorySize()
                    ? history.subList(history.size() - properties.getHistorySize(), history.size())
                    : history;
            for (HistoryMessage h : tail) {
                if (h.content() == null || h.content().isBlank()) {
                    continue;
                }
                if ("user".equals(h.role())) {
                    messages.add(Message.user(h.content().trim()));
                    kept++;
                } else if ("assistant".equals(h.role())) {
                    messages.add(Message.assistant(h.content().trim()));
                    kept++;
                }
                // 其他角色一律丢弃，防止前端注入 system/tool 消息
            }
        }
        messages.add(Message.user(userMessage));
        return messages;
    }
}
