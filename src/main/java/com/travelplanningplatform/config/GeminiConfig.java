package com.travelplanningplatform.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Bean
    @ConditionalOnProperty(name = "gemini.api.key")
    public Client geminiClient() {
        return Client.builder().apiKey(apiKey).build();
    }
}

