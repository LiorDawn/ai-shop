package org.example.aishop.ai.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 重试处理器（运维支撑层）
 *
 * 提供指数退避重试机制，用于：
 * 1. Schema 校验失败时重试 LLM 调用
 * 2. Embedding API 调用失败重试
 *
 * 退避策略：第 i 次重试等待 2^i 秒（1s → 2s → 4s）
 */
@Component
public class RetryHandler {

    private static final Logger log = LoggerFactory.getLogger(RetryHandler.class);

    /** 最大重试次数 */
    private static final int DEFAULT_MAX_RETRIES = 3;
    /** 初始退避延迟（毫秒） */
    private static final long INITIAL_BACKOFF_MS = 1000;
    /** 退避指数 */
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * 指数退避等待
     *
     * @param attempt 当前重试次数（0-based）
     */
    public void backoff(int attempt) {
        long delay = (long) (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, attempt));
        log.info("指数退避等待: attempt={}, delay={}ms", attempt + 1, delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("退避等待被中断");
        }
    }

    /**
     * 带重试的执行器
     *
     * @param operation 需要重试的操作
     * @param phaseName 阶段名称（用于日志）
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws RuntimeException 重试耗尽后抛出最后一次异常
     */
    public <T> T executeWithRetry(RetryableOperation<T> operation, String phaseName) {
        return executeWithRetry(operation, phaseName, DEFAULT_MAX_RETRIES);
    }

    /**
     * 带重试的执行器（指定最大重试次数）
     */
    public <T> T executeWithRetry(RetryableOperation<T> operation, String phaseName, int maxRetries) {
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                log.warn("[RETRY] phase={}, attempt={}/{}, error={}",
                        phaseName, i + 1, maxRetries, e.getMessage());

                if (i < maxRetries - 1) {
                    backoff(i);
                }
            }
        }

        throw new RuntimeException(
                String.format("重试耗尽: phase=%s, maxRetries=%d, lastError=%s",
                        phaseName, maxRetries, lastException != null ? lastException.getMessage() : "unknown"),
                lastException);
    }

    /**
     * 可重试的操作接口
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}