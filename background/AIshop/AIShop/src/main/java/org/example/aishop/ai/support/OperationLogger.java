package org.example.aishop.ai.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志记录器（运维支撑层）
 *
 * 统一记录 AI 工具调用、Schema 校验、编排执行等关键操作的日志。
 * 日志格式标准化，便于 ELK / 日志平台采集分析。
 */
@Component
public class OperationLogger {

    private static final Logger log = LoggerFactory.getLogger("AI_OPERATION");
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 记录工具执行日志
     */
    public void logToolExecution(String toolName, boolean success, String message) {
        String status = success ? "SUCCESS" : "FAILURE";
        log.info("[TOOL_EXEC] tool={}, status={}, message={}", toolName, status, message);
    }

    /**
     * 记录 Schema 校验日志
     */
    public void logSchemaValidation(boolean success, String reason, long elapsedMs) {
        String status = success ? "PASS" : "FAIL";
        log.info("[SCHEMA_VALID] status={}, reason={}, elapsed={}ms", status, reason, elapsedMs);
    }

    /**
     * 记录编排执行完成日志
     */
    public void logOrchestrationComplete(Long userId, Long sessionId, int toolCallCount, long totalElapsedMs) {
        log.info("[ORCHESTRATION_COMPLETE] userId={}, sessionId={}, toolCalls={}, totalElapsed={}ms",
                userId, sessionId, toolCallCount, totalElapsedMs);
    }

    /**
     * 记录 LLM 调用日志
     */
    public void logLLMCall(String phase, int messageCount, long elapsedMs) {
        log.info("[LLM_CALL] phase={}, messages={}, elapsed={}ms", phase, messageCount, elapsedMs);
    }

    /**
     * 记录重试日志
     */
    public void logRetry(String phase, int attempt, int maxAttempts, String reason) {
        log.warn("[RETRY] phase={}, attempt={}/{}, reason={}", phase, attempt, maxAttempts, reason);
    }
}