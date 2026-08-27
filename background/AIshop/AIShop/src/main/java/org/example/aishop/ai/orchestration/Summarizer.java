package org.example.aishop.ai.orchestration;

import dev.langchain4j.data.message.ChatMessage;
import org.example.aishop.dto.ai.ToolCallResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 输出层接口 — LLM 总结、SSE 流式推送、会话落地
 *
 * 职责：
 * 1. 构建二次 LLM 汇总 Prompt
 * 2. SSE 流式逐字推送回复给前端
 * 3. 完整会话持久化（用户消息 + 工具日志 + AI 回复）
 * 4. 失败兜底回复生成
 */
public interface Summarizer {

    /**
     * SSE 流式输出：整合工具结果 → 调用 LLM 生成自然语言 → 逐字推送
     *
     * @param sessionId    当前会话 ID（用于持久化）
     * @param messages     完整消息上下文
     * @param userMessage  用户原始消息
     * @param toolResults  工具执行记录
     * @param emitter      SSE 连接
     * @param disconnected 断连标记
     */
    void streamReply(Long sessionId, List<ChatMessage> messages, String userMessage,
                     List<ToolCallResult> toolResults, SseEmitter emitter, boolean[] disconnected);

    /**
     * 完整会话持久化
     */
    void persistConversation(Long sessionId, String userMessage, String reply,
                             String imgUrl, List<ToolCallResult> toolResults);

    /**
     * 生成失败兜底回复
     */
    String buildFallback(String userMessage, List<ToolCallResult> toolResults);
}