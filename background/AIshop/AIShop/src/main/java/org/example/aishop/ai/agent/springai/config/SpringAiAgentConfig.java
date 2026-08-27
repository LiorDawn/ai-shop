package org.example.aishop.ai.agent.springai.config;

import org.example.aishop.ai.agent.springai.AiAgentProvider;
import org.example.aishop.ai.agent.springai.ShoppingAssistantAgent;
import org.example.aishop.ai.orchestration.AIChatAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI Agent 配置
 *
 * 职责：
 * 1. 创建 Spring AI OpenAiChatModel（复用 ai.chat.* 配置）
 * 2. 创建 ChatClient Bean（Spring AI 统一入口）
 * 3. 根据 ai.provider 配置切换新旧 Agent 实现
 */
@Configuration
public class SpringAiAgentConfig {

    // ================== Spring AI ChatClient ==================

    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.provider", havingValue = "spring-ai", matchIfMissing = false)
    public OpenAiApi openAiApi(
            @Value("${ai.chat.base-url}") String baseUrl,
            @Value("${ai.chat.api-key}") String apiKey) {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.provider", havingValue = "spring-ai", matchIfMissing = false)
    public OpenAiChatModel openAiChatModel(
            OpenAiApi openAiApi,
            @Value("${ai.chat.model}") String model,
            @Value("${ai.chat.max-tokens}") int maxTokens,
            @Value("${ai.chat.temperature}") double temperature) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "spring-ai", matchIfMissing = false)
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    // ================== Agent 切换 ==================

    @Bean
    @Primary
    public AiAgentProvider aiAgentProvider(
            @Autowired(required = false) @Qualifier("springAiAgent") ShoppingAssistantAgent springAiAgent,
            @Autowired AIChatAgent langchain4jAgent,
            @Value("${ai.provider:langchain4j}") String provider) {
        if ("spring-ai".equals(provider) && springAiAgent != null) {
            return springAiAgent;
        }
        return langchain4jAgent;
    }
}