package com.lc.kbagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 通用 Bean 配置。
 */
@Configuration
public class AppConfig {

    /**
     * 供各工具使用的通用 HTTP 客户端（如天气工具调用第三方接口）。
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
