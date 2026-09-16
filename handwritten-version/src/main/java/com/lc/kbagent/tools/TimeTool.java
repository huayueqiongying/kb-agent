package com.lc.kbagent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 工具：获取当前日期时间（北京时间）。
 */
@Component
public class TimeTool implements Tool {

    @Override
    public String name() {
        return "getCurrentTime";
    }

    @Override
    public String description() {
        return "获取当前日期和时间（北京时间）。当用户询问现在几点、今天星期几、今天是几号等时间类问题时使用。无需参数。";
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        params.set("properties", JsonNodeFactory.instance.objectNode());
        params.set("required", JsonNodeFactory.instance.arrayNode());
        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        String now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE HH:mm:ss", Locale.CHINA));
        return "当前时间（北京时间）：" + now;
    }
}
