package com.lc.kbagent.web;

import com.lc.kbagent.agent.AgentException;
import com.lc.kbagent.agent.AgentReply;
import com.lc.kbagent.agent.AgentService;
import com.lc.kbagent.config.LlmProperties;
import com.lc.kbagent.tools.Tool;
import com.lc.kbagent.tools.ToolRegistry;
import com.lc.kbagent.web.dto.ChatRequest;
import com.lc.kbagent.web.dto.HistoryMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Agent 对外 HTTP 接口。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentService agentService;
    private final ToolRegistry toolRegistry;
    private final LlmProperties llmProperties;

    public ChatController(AgentService agentService, ToolRegistry toolRegistry, LlmProperties llmProperties) {
        this.agentService = agentService;
        this.toolRegistry = toolRegistry;
        this.llmProperties = llmProperties;
    }

    /**
     * 发送一条消息给 Agent，返回最终回答与工具调用过程。
     */
    @PostMapping("/chat")
    public AgentReply chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("message 不能为空");
        }
        List<HistoryMessage> history = request.history() == null ? List.of() : request.history();
        return agentService.chat(request.message().trim(), history);
    }

    /**
     * 查看当前 Agent 可用的工具（调试 / 演示用）。
     */
    @GetMapping("/tools")
    public List<Map<String, String>> tools() {
        return toolRegistry.all().stream()
                .map(t -> Map.of(
                        "name", t.name(),
                        "description", t.description()))
                .toList();
    }

    /**
     * 健康检查。
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "model", llmProperties.getModel(),
                "apiKeyConfigured", llmProperties.getApiKey() != null && !llmProperties.getApiKey().isBlank(),
                "toolCount", toolRegistry.all().size());
    }

    /** 参数错误 → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    /** Agent 运行错误（如未配置 Key、大模型调用失败）→ 500 + 友好提示 */
    @ExceptionHandler(AgentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleAgentError(AgentException e) {
        return Map.of("error", e.getMessage());
    }
}
