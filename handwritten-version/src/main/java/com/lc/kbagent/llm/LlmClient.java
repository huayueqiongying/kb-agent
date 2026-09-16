package com.lc.kbagent.llm;

import com.lc.kbagent.llm.dto.ChatResponse;
import com.lc.kbagent.llm.dto.Message;
import com.lc.kbagent.llm.dto.ToolDef;

import java.util.List;

/**
 * 大模型聊天接口抽象。定义成接口便于测试时注入桩实现，隔离外部网络依赖。
 */
public interface LlmClient {

    /**
     * 携带消息列表与可用工具列表，向大模型发起一次对话。
     *
     * @param messages 历史消息 + 本次输入
     * @param tools    当前可用的工具定义
     * @return 模型响应，可能携带 tool_calls（要求调用工具）
     */
    ChatResponse chat(List<Message> messages, List<ToolDef> tools);
}
