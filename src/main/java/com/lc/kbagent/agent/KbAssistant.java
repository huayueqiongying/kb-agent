package com.lc.kbagent.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent 服务接口：LangChain4j 的 AiServices 会根据本接口在运行时生成动态实现，
 * 自动完成「模型调用 → 工具执行 → 结果回填 → 再次调用模型」的完整 Agent 循环，
 * 并通过 ChatMemory 维护多轮会话记忆。
 */
public interface KbAssistant {

    @SystemMessage("""
            你是「星辰科技」的智能知识库问答助手，负责根据企业内部知识库回答员工与客户的问题。

            回答规则：
            1. 回答与公司制度、产品、政策、流程相关的问题时，必须先调用 searchKnowledgeBase 工具检索知识库，并基于检索到的内容作答，禁止编造知识库中不存在的信息。
            2. 引用知识库内容时，请说明信息来自哪个文档（如"根据《员工手册》"）。
            3. 涉及"当前时间/日期"的问题，调用 getCurrentTime 获取准确时间；涉及数学计算的问题，调用 calculate 工具；涉及天气的问题，调用 getWeather 工具。
            4. 如果检索不到相关信息，请如实告知"知识库中暂未找到相关内容"，并建议用户联系行政部或查看 OA 系统，不要强行编造。
            5. 回答保持简洁、友好、专业，使用中文，一般不超过 200 字。
            """)
    String chat(@UserMessage String userMessage);
}
