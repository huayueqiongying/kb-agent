package com.lc.kbagent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lc.kbagent.config.ToolProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

/**
 * 工具：查询城市天气。
 *
 * <p>两种模式（application.yml 中 tools.weather.mock 控制）：
 * <ul>
 *   <li>mock=true（默认）：返回基于城市名+日期的模拟数据，离线可演示、结果稳定；</li>
 *   <li>mock=false：调用 Open-Meteo 免费接口（无需 API Key）返回真实天气，失败时自动回退模拟数据。</li>
 * </ul>
 */
@Component
public class WeatherTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] CONDITIONS = {"晴", "多云", "阴", "小雨", "晴转多云", "微风"};

    private final ToolProperties properties;
    private final RestClient restClient;

    public WeatherTool(ToolProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String name() {
        return "getWeather";
    }

    @Override
    public String description() {
        return "查询指定城市的今日天气与气温（摄氏度）。当用户询问某地天气、气温、要不要带伞等问题时使用。参数 city 为城市名，如 北京、上海、深圳。";
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        ObjectNode city = JsonNodeFactory.instance.objectNode();
        city.put("type", "string");
        city.put("description", "城市名，例如：北京、上海、深圳");
        properties.set("city", city);
        params.set("properties", properties);
        params.set("required", JsonNodeFactory.instance.arrayNode().add("city"));
        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        String city = parseCity(argumentsJson);
        if (city == null || city.isBlank()) {
            return "参数解析失败：请提供 city 参数，例如 {\"city\": \"北京\"}";
        }
        if (properties.getWeather().isMock()) {
            return mockWeather(city.trim());
        }
        try {
            return realWeather(city.trim());
        } catch (Exception e) {
            // 真实接口失败时回退模拟数据，保证 Agent 对话不被网络问题中断
            return mockWeather(city.trim()) + "\n（注：实时天气获取失败，以上为模拟数据）";
        }
    }

    private String parseCity(String argumentsJson) {
        try {
            JsonNode node = MAPPER.readTree(argumentsJson);
            JsonNode city = node.path("city");
            return city.isTextual() ? city.asText() : null;
        } catch (Exception e) {
            return null;
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
        ResponseEntity<JsonNode> geoResp = restClient.get()
                .uri("https://geocoding-api.open-meteo.com/v1/search?name={city}&count=1&language=zh&format=json", city)
                .retrieve()
                .toEntity(JsonNode.class);
        JsonNode geo = geoResp.getBody();
        JsonNode first = geo == null ? null : geo.path("results").path(0);
        if (first == null || first.isMissingNode()) {
            throw new IllegalStateException("未找到城市：" + city);
        }
        double latitude = first.path("latitude").asDouble();
        double longitude = first.path("longitude").asDouble();

        // 2. 查实时天气与今日最高/最低温
        ResponseEntity<JsonNode> weatherResp = restClient.get()
                .uri("https://api.open-meteo.com/v1/forecast"
                        + "?latitude={lat}&longitude={lon}"
                        + "&current_weather=true&daily=temperature_2m_max,temperature_2m_min"
                        + "&timezone=auto", latitude, longitude)
                .retrieve()
                .toEntity(JsonNode.class);
        JsonNode weather = weatherResp.getBody();
        if (weather == null) {
            throw new IllegalStateException("天气接口返回为空");
        }

        JsonNode current = weather.path("current_weather");
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
}
