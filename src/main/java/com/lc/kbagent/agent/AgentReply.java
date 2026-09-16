package com.lc.kbagent.agent;

import java.util.List;

/**
 * Agent 一次对话的返回结果。
 *
 * @param reply     最终回答文本
 * @param toolsUsed 本次实际调用的工具名列表（供前端展示"思考过程"）
 * @param status    ok=正常回答
 */
public record AgentReply(
        String reply,
        List<String> toolsUsed,
        String status
) {
}
