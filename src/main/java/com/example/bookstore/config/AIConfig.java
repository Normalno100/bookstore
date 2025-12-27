package com.example.bookstore.config;

import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для Spring AI
 * ChatModel автоматически создается Spring Boot через spring-ai-starter-model-gigachat
 *
 * Если автоконфигурация не работает, раскомментируйте и настройте вручную:
 */
@Configuration
public class AIConfig {

    // GigaChat должен автоматически создать bean через auto-configuration
    // Параметры берутся из application.yml:
    // spring.ai.gigachat.auth.bearer.api-key
    // spring.ai.gigachat.model
    // spring.ai.gigachat.base-url

    /* Если автоконфигурация не работает, используйте этот код:

    @Bean
    public ChatModel gigaChatModel(
            @Value("${spring.ai.gigachat.base-url}") String baseUrl,
            @Value("${spring.ai.gigachat.auth.bearer.api-key}") String apiKey,
            @Value("${spring.ai.gigachat.model}") String model) {

        GigaChatConnectionProperties properties = new GigaChatConnectionProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiKey(apiKey);
        properties.setModel(model);

        return new GigaChatClient(properties);
    }
    */
}