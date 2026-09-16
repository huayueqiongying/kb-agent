# handwritten-version（v1 · 手写 Function Calling 版）

这是本项目的 v1 版本：**不依赖 LangChain4j，自研 OpenAI 兼容协议客户端 + 手写 Agent 循环**。

## 保留价值

- 完整展示了 Function Calling 协议原理（请求/响应 DTO、tool_calls 解析、tool 消息回填）；
- 面试被问到"Agent 循环怎么实现"时，可对照本版代码讲清协议细节；
- 生产若不想引入框架，本版可直接使用（约 1500 行，零额外依赖）。

## 与本版（主项目 LangChain4j 版）的差异

| 维度 | v1 手写版 | 主版本（LangChain4j） |
|---|---|---|
| LLM 调用 | 自研 `OpenAiLlmClient`（RestClient + OpenAI 协议） | `OpenAiChatModel` |
| Agent 循环 | 自研 `AgentService` while 循环 | `AiServices` 自动完成 |
| 工具机制 | `Tool` 接口 + `ToolRegistry` | `@Tool` 注解方法 |
| RAG 检索 | `TfidfIndex` | `TfidfIndex` + `TfidfContentRetriever`（相同检索核心） |

## 如何运行

与主项目相同：填写 `src/main/resources/application.yml` 的 `llm.api-key` 后 `mvn spring-boot:run`。

> 提示：主项目的 `docs/面试讲解.md` 中「技术选型」一节给出了两版取舍的完整话术。
