package com.lc.kbagent.tools;

import com.lc.kbagent.config.ToolProperties;
import com.lc.kbagent.rag.TfidfIndex;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Agent 可用工具集合。
 *
 * <p>方法上的 {@link Tool} 注解会被 LangChain4j 自动解析为
 * Function Calling 的工具定义（名称、描述、参数 JSON Schema），
 * 并在模型请求调用时自动执行——无需自己维护 tool_calls 循环。
 * 新增工具 = 新增一个带 {@link Tool} 注解的方法，其余代码零改动。
 */
@Component
public class AgentTools {

    private static final String[] CONDITIONS = {"晴", "多云", "阴", "小雨", "晴转多云", "微风"};
    private static final int TOP_K = 3;

    private final TfidfIndex index;
    private final ToolProperties toolProperties;
    private final RestClient restClient;
    private final ToolCallTracker tracker;

    public AgentTools(TfidfIndex index,
                      ToolProperties toolProperties,
                      RestClient restClient,
                      ToolCallTracker tracker) {
        this.index = index;
        this.toolProperties = toolProperties;
        this.restClient = restClient;
        this.tracker = tracker;
    }

    /* ========== 工具 1：知识库检索（RAG） ========== */

    @Tool(name = "searchKnowledgeBase",
            value = "在企业知识库中检索与用户问题最相关的文档片段。"
                    + "回答关于公司制度、员工手册、产品功能、价格、售后等知识库问题时，必须先调用本工具，"
                    + "再基于检索结果回答。参数 query 为检索关键词或问题核心内容。")
    public String searchKnowledgeBase(String query) {
        tracker.record("searchKnowledgeBase");
        if (query == null || query.isBlank()) {
            return "参数解析失败：请提供 query 参数，例如 {\"query\": \"年假\"}";
        }
        var hits = index.search(query.trim(), TOP_K);
        if (hits.isEmpty()) {
            return "知识库检索结果：未检索到与「" + query + "」相关的内容。";
        }
        StringBuilder sb = new StringBuilder("知识库检索结果（按相关度从高到低）：\n");
        for (int i = 0; i < hits.size(); i++) {
            TfidfIndex.Hit hit = hits.get(i);
            sb.append("【").append(i + 1).append("】来源文档：").append(hit.chunk().docName())
                    .append("｜相关度：").append(String.format("%.2f", hit.score())).append("\n");
            sb.append(hit.chunk().content()).append("\n");
        }
        return sb.toString();
    }

    /* ========== 工具 2：当前时间 ========== */

    @Tool(name = "getCurrentTime",
            value = "获取当前日期和时间（北京时间）。当用户询问现在几点、今天星期几、今天是几号等时间类问题时使用。无需参数。")
    public String getCurrentTime() {
        tracker.record("getCurrentTime");
        String now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE HH:mm:ss", Locale.CHINA));
        return "当前时间（北京时间）：" + now;
    }

    /* ========== 工具 3：安全计算器 ========== */

    @Tool(name = "calculate",
            value = "执行数学四则运算，支持括号与小数。当用户需要精确计算结果时使用，例如 (1+2)*3、100/4、2.5*8。参数 expression 为运算表达式。")
    public String calculate(String expression) {
        tracker.record("calculate");
        if (expression == null || expression.isBlank()) {
            return "参数解析失败：请提供 expression 参数，例如 {\"expression\": \"(1+2)*3\"}";
        }
        if (!ExpressionCalculator.isValidExpression(expression)) {
            return "表达式不合法：仅支持数字与 + - * / ( ) 运算，例如 (1+2)*3";
        }
        try {
            return "计算结果：" + expression + " = " + ExpressionCalculator.eval(expression);
        } catch (Exception e) {
            return "表达式无法解析，请检查语法是否正确，例如 (1+2)*3";
        }
    }

    /* ========== 工具 4：天气查询 ========== */

    @Tool(name = "getWeather",
            value = "查询指定城市的今日天气与气温（摄氏度）。当用户询问某地天气、气温、要不要带伞等问题时使用。参数 city 为城市名，如 北京、上海、深圳。")
    public String getWeather(String city) {
        tracker.record("getWeather");
        if (city == null || city.isBlank()) {
            return "参数解析失败：请提供 city 参数，例如 {\"city\": \"北京\"}";
        }
        if (toolProperties.getWeather().isMock()) {
            return mockWeather(city.trim());
        }
        try {
            return realWeather(city.trim());
        } catch (Exception e) {
            // 真实接口失败时回退模拟数据，保证 Agent 对话不被网络问题中断
            return mockWeather(city.trim()) + "\n（注：实时天气获取失败，以上为模拟数据）";
        }
    }

    /** 模拟数据：由城市名 + 年内第几天决定，同一天同一城市结果稳定，便于演示讲解 */
    private String mockWeather(String city) {
        long seed = (city.hashCode() & 0x7fffffff) + LocalDate.now().getDayOfYear();
        String condition = CONDITIONS[(int) (seed % CONDITIONS.length)];
        int low = 8 + (int) (seed % 18);
        int high = low + 6 + (int) ((seed / 7) % 8);
        int wind = 1 + (int) (seed % 5);
        return "【演示模式·模拟数据】" + city + "今日天气：" + condition + "，气温 " + low
                + "~" + high + "℃，风力 " + wind + " 级。\n"
                + "（如需真实天气，请在 application.yml 中设置 tools.weather.mock=false）";
    }

    /** 真实天气：Open-Meteo 免费接口，先地理编码取经纬度，再查实时天气 */
    private String realWeather(String city) throws Exception {
        // 1. 城市 → 经纬度
        ResponseEntity<com.fasterxml.jackson.databind.JsonNode> geoResp = restClient.get()
                .uri("https://geocoding-api.open-meteo.com/v1/search?name={city}&count=1&language=zh&format=json", city)
                .retrieve()
                .toEntity(com.fasterxml.jackson.databind.JsonNode.class);
        com.fasterxml.jackson.databind.JsonNode geo = geoResp.getBody();
        com.fasterxml.jackson.databind.JsonNode first = geo == null ? null : geo.path("results").path(0);
        if (first == null || first.isMissingNode()) {
            throw new IllegalStateException("未找到城市：" + city);
        }
        double latitude = first.path("latitude").asDouble();
        double longitude = first.path("longitude").asDouble();

        // 2. 实时天气 + 今日最高/最低温
        ResponseEntity<com.fasterxml.jackson.databind.JsonNode> weatherResp = restClient.get()
                .uri("https://api.open-meteo.com/v1/forecast"
                        + "?latitude={lat}&longitude={lon}"
                        + "&current_weather=true&daily=temperature_2m_max,temperature_2m_min"
                        + "&timezone=auto", latitude, longitude)
                .retrieve()
                .toEntity(com.fasterxml.jackson.databind.JsonNode.class);
        com.fasterxml.jackson.databind.JsonNode weather = weatherResp.getBody();
        if (weather == null) {
            throw new IllegalStateException("天气接口返回为空");
        }
        com.fasterxml.jackson.databind.JsonNode current = weather.path("current_weather");
        double currentTemp = current.path("temperature").asDouble();
        int weatherCode = current.path("weathercode").asInt();
        double max = weather.path("daily").path("temperature_2m_max").path(0).asDouble();
        double min = weather.path("daily").path("temperature_2m_min").path(0).asDouble();

        return city + "今日天气：" + codeToText(weatherCode) + "，当前气温 " + round(currentTemp)
                + "℃，今日气温 " + round(min) + "~" + round(max) + "℃（数据来源：Open-Meteo）。";
    }

    /** WMO 天气代码 → 中文描述 */
    private String codeToText(int code) {
        if (code == 0) {
            return "晴";
        }
        if (code <= 3) {
            return "多云";
        }
        if (code == 45 || code == 48) {
            return "有雾";
        }
        if (code <= 67) {
            return "有雨";
        }
        if (code <= 77) {
            return "有雪";
        }
        if (code <= 82) {
            return "阵雨";
        }
        return "有雷阵雨";
    }

    private String round(double value) {
        return String.valueOf(Math.round(value * 10) / 10.0);
    }

    /* ========== 工具列表（管理接口展示用） ========== */

    /** 反射读取全部 @Tool 方法，返回工具名与描述（value 为 String[]，拼接展示） */
    public List<Map<String, String>> describeTools() {
        List<Map<String, String>> result = new ArrayList<>();
        for (Method method : getClass().getMethods()) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool != null) {
                result.add(Map.of(
                        "name", tool.name(),
                        "description", String.join(" ", tool.value())));
            }
        }
        return result;
    }
}
