package com.example.caseprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient
 */
@Configuration
public class WebClientConfig {

    @Value("${case.api.url}")
    private String apiBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(apiBaseUrl)
                .build();
    }
}