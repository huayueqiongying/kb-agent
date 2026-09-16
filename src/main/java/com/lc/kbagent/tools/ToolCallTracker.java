package com.lc.kbagent.tools;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求级工具调用记录器：在一次 Agent 对话内记录实际调用了哪些工具，
 * 用于前端展示"思考过程"（如"工具：知识库检索"）。
 *
 * <p>实现说明：使用 ThreadLocal 实现请求级隔离（演示方案）。
 * 多用户生产环境应改为按会话（sessionId）维度记录。
 */
@Component
public class ToolCallTracker {

    private static final ThreadLocal<List<String>> CURRENT = new ThreadLocal<>();

    /** 开始记录一次对话 */
    public void start() {
        CURRENT.set(new ArrayList<>());
    }

    /** 工具执行时调用，记录工具名 */
    public void record(String toolName) {
        List<String> list = CURRENT.get();
        if (list != null) {
            list.add(toolName);
        }
    }

    /** 返回本次对话调用的工具名列表（不可变副本） */
    public List<String> tools() {
        List<String> list = CURRENT.get();
        return list == null ? List.of() : List.copyOf(list);
    }

    /** 对话结束清理，避免线程复用导致串数据 */
    public void clear() {
        CURRENT.remove();
    }
}
