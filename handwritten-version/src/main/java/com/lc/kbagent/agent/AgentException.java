package com.lc.kbagent.agent;

/**
 * Agent 运行期业务异常，用于向调用方返回友好错误信息。
 */
public class AgentException extends RuntimeException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
