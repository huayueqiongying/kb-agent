package com.lc.kbagent.tools;

import com.lc.kbagent.llm.dto.FunctionDef;
import com.lc.kbagent.llm.dto.ToolDef;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心：Spring 启动时自动收集所有 Tool 实现，
 * 既负责按名字查找工具，也负责把工具定义转换成大模型需要的格式。
 */
@Component
public class ToolRegistry {

    private final List<Tool> tools;
    private final Map<String, Tool> byName = new HashMap<>();

    public ToolRegistry(List<Tool> tools) {
        this.tools = tools;
        for (Tool tool : tools) {
            if (byName.put(tool.name(), tool) != null) {
                throw new IllegalStateException("存在重名工具：" + tool.name());
            }
        }
    }

    /** 按名字获取工具，不存在返回 null */
    public Tool get(String name) {
        return byName.get(name);
    }

    /** 全部工具定义（发给大模型用） */
    public List<ToolDef> toolDefs() {
        return tools.stream()
                .map(t -> new ToolDef("function",
                        new FunctionDef(t.name(), t.description(), t.parametersSchema())))
                .toList();
    }

    /** 全部工具（供管理接口展示） */
    public List<Tool> all() {
        return tools;
    }
}
