package org.example.aishop.ai.orchestration.impl;

import dev.langchain4j.data.message.ChatMessage;
import org.example.aishop.dto.ai.ChatRequestV2;
import org.example.aishop.ai.orchestration.Memory;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.ai.storage.postgres.PgVectorStoreService;
import org.example.aishop.ai.storage.redis.RedisConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 记忆层实现 — 上下文构建、历史截断、RAG 检索增强
 */
@Component
public class DefaultMemory implements Memory {

    private static final Logger log = LoggerFactory.getLogger(DefaultMemory.class);
    private static final int MAX_HISTORY_ROUNDS = 10;
    /** RAG 检索超时（秒），防止 PgVector 不可用时阻塞请求 */
    private static final int RAG_TIMEOUT_SECONDS = 5;
    /** RAG 连续失败次数（用于熔断） */
    private final AtomicInteger ragFailCount = new AtomicInteger(0);
    private static final int RAG_CIRCUIT_BREAKER_THRESHOLD = 3;

    @Autowired
    private RedisConversationService redisConversationService;

    /**
     * pgvector 服务为可选注入：
     * 设置 pgvector.enabled=false 时该 Bean 不存在，AI 对话跳过 RAG 商品检索，
     * 后续再通过已有 Redis 向量补全即可。
     */
    @Autowired(required = false)
    private PgVectorStoreService pgVectorStoreService;

    @Override
    public String buildSystemPrompt(Long userId, ChatRequestV2 request, boolean needsTool) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能购物助手，可以帮助用户推荐商品、查询订单、管理购物车、处理售后。");

        // RAG 检索增强（带超时+熔断保护；pgvector 禁用时跳过）
        if (pgVectorStoreService != null
                && request.getRagEnabled() != null && request.getRagEnabled()
                && ragFailCount.get() < RAG_CIRCUIT_BREAKER_THRESHOLD) {
            try {
                String ragContext = CompletableFuture
                        .supplyAsync(() -> pgVectorStoreService.search(request.getMessage()))
                        .get(RAG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (ragContext != null && !ragContext.isEmpty()) {
                    prompt.append("\n\n").append(ragContext);
                    ragFailCount.set(0);  // 成功则重置
                }
            } catch (TimeoutException e) {
                int fails = ragFailCount.incrementAndGet();
                log.warn("RAG 检索超时 ({}s)，熔断计数: {}/{}", RAG_TIMEOUT_SECONDS, fails, RAG_CIRCUIT_BREAKER_THRESHOLD);
            } catch (Exception e) {
                int fails = ragFailCount.incrementAndGet();
                log.warn("RAG 检索异常: {}，熔断计数: {}/{}", e.getMessage(), fails, RAG_CIRCUIT_BREAKER_THRESHOLD);
            }
        }

        // 工具 Schema（仅当有工具意图时注入）
        if (needsTool) {
            prompt.append("\n\n").append(ToolCallSchema.buildToolSchemaPrompt());
        }

        return prompt.toString();
    }

    @Override
    public List<ChatMessage> loadHistory(Long sessionId) {
        List<ChatMessage> history = redisConversationService.loadContext(sessionId);
        return trimHistory(history);
    }

    private List<ChatMessage> trimHistory(List<ChatMessage> history) {
        if (history.size() <= MAX_HISTORY_ROUNDS * 2) {
            return history;
        }
        int fromIndex = history.size() - (MAX_HISTORY_ROUNDS * 2);
        return history.subList(Math.max(0, fromIndex), history.size());
    }

    @Override
    public int estimateContextLength(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage msg : messages) {
            total += msg.text().length();
        }
        return total;
    }

    @Override
    public void updateContext(Long sessionId, String userMessage, String reply, String imgUrl) {
        redisConversationService.updateContext(sessionId, userMessage, reply, imgUrl);
    }
}