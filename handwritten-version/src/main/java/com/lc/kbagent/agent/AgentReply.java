package com.lc.kbagent.agent;

import com.lc.kbagent.llm.dto.ChatResponse.Usage;

import java.util.List;

/**
 * Agent 一次对话的返回结果。
 *
 * @param reply      最终回答文本
 * @param toolsUsed  本次实际调用的工具名列表（供前端展示"思考过程"）
 * @param iterations 模型↔工具循环轮数
 * @param usage      token 消耗统计
 * @param status     ok=正常回答；max_iterations=达到最大循环轮数提前结束
 */
public record AgentReply(
        String reply,
        List<String> toolsUsed,
        int iterations,
        Usage usage,
        String status
) {
}
