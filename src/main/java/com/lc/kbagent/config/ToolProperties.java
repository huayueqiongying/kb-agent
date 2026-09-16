package com.lc.kbagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工具配置（application.yml 中 tools.* 前缀）。
 */
@ConfigurationProperties(prefix = "tools")
public class ToolProperties {

    private Weather weather = new Weather();

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public static class Weather {

        /** true 返回模拟天气（离线演示稳定）；false 调用 Open-Meteo 免费接口 */
        private boolean mock = true;

        public boolean isMock() {
            return mock;
        }

        public void setMock(boolean mock) {
            this.mock = mock;
        }
    }
}
