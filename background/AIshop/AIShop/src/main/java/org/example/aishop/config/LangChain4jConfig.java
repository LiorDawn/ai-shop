package org.example.aishop.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.ai4j.openai4j.OpenAiHttpException;
import org.example.aishop.ai.rag.RedisBackedEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * LangChain4j 模型配置
 *
 * 统一管理 LLM 模型 Bean，所有 AI 大模型调用均通过 LangChain4j 框架，
 * 包括：Chat（对话）、StreamingChat（流式对话）、Embedding（向量化）、
 *        EmbeddingStore（持久化向量存储）
 */
@Configuration
@EnableRetry
public class LangChain4jConfig {

    // ==================== Chat 模型 ====================

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${ai.chat.base-url}") String baseUrl,
            @Value("${ai.chat.api-key}") String apiKey,
            @Value("${ai.chat.model}") String model,
            @Value("${ai.chat.max-tokens}") int maxTokens,
            @Value("${ai.chat.temperature}") double temperature) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 工具决策专用 Chat 模型 — maxTokens=256，加速工具调用决策
     * 工具决策只需输出短 JSON，不需要 2000 token
     */
    @Bean("plannerChatModel")
    public ChatLanguageModel plannerChatModel(
            @Value("${ai.chat.base-url}") String baseUrl,
            @Value("${ai.chat.api-key}") String apiKey,
            @Value("${ai.chat.model}") String model,
            @Value("${ai.chat.temperature}") double temperature) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(256)  // ★ 工具决策只需短 JSON
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(
            @Value("${ai.chat.base-url}") String baseUrl,
            @Value("${ai.chat.api-key}") String apiKey,
            @Value("${ai.chat.model}") String model,
            @Value("${ai.chat.max-tokens}") int maxTokens,
            @Value("${ai.chat.temperature}") double temperature) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // ==================== Embedding 模型 ====================

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${ai.chat.base-url}") String baseUrl,
            @Value("${ai.chat.api-key}") String apiKey,
            @Value("${ai.rag.embedding-model:text-embedding-v3}") String embeddingModel) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(30))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // ==================== 持久化向量存储 ====================

    /**
     * Redis 持久化向量存储（替代 InMemoryEmbeddingStore）
     *
     * 特性：
     * - 服务重启不丢失（Redis 持久化）
     * - 集群多实例共享（同一 Redis 实例）
     * - 向量不占 JVM 堆内存
     * - TTL 自动过期
     */
    @Bean
    public RedisBackedEmbeddingStore redisBackedEmbeddingStore(
            StringRedisTemplate stringRedisTemplate,
            @Value("${ai.rag.knowledge-ttl:3600}") long knowledgeTtl) {
        return new RedisBackedEmbeddingStore(stringRedisTemplate, knowledgeTtl);
    }

    // ==================== 线程池 ====================

    @Bean("aiStreamTaskExecutor")
    public ThreadPoolTaskExecutor aiStreamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ai-stream-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}