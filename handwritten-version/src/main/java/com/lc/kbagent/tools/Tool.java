package com.lc.kbagent.tools;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具抽象：一个工具 = 名称 + 给模型的描述 + 参数 JSON Schema + 执行逻辑。
 *
 * <p>新增一个工具只需实现本接口并注册为 Spring Bean，即可自动暴露给大模型调用，
 * 无需改动 Agent 循环，体现了"面向扩展开放"的设计。
 */
public interface Tool {

    /** 工具名（英文，作为 Function Calling 的 function name） */
    String name();

    /** 给大模型看的描述，说明何时使用、如何使用 */
    String description();

    /** 参数 JSON Schema，例如 {"type":"object","properties":{...},"required":[...]} */
    JsonNode parametersSchema();

    /**
     * 执行工具。
     *
     * @param argumentsJson 大模型按 Schema 生成的参数 JSON 字符串
     * @return 返回给大模型的文本结果（会被追加为 tool 消息）
     */
    String execute(String argumentsJson);
}
