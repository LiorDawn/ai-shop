package org.example.aishop.ai.orchestration;

import dev.langchain4j.data.message.ChatMessage;
import org.example.aishop.dto.ai.ChatRequestV2;

import java.util.List;

/**
 * 记忆层接口 — 上下文构建、会话历史管理、RAG 检索增强
 *
 * 职责：
 * 1. 构建 System Prompt（角色设定 + RAG 商品知识 + 工具 Schema）
 * 2. 加载 & 截断历史对话上下文
 * 3. 上下文长度估算
 */
public interface Memory {

    /**
     * 构建完整的 System Prompt
     *
     * @param userId    用户 ID
     * @param request   请求参数（含 RAG/工具开关）
     * @param needsTool 是否需要工具调用（决定是否注入工具 Schema）
     * @return System Prompt 文本
     */
    String buildSystemPrompt(Long userId, ChatRequestV2 request, boolean needsTool);

    /**
     * 加载历史对话上下文（自动截断到最近 N 轮）
     *
     * @param sessionId 会话 ID
     * @return 历史消息列表
     */
    List<ChatMessage> loadHistory(Long sessionId);

    /**
     * 估算上下文总字符数（用于熔断判断）
     */
    int estimateContextLength(List<ChatMessage> messages);

    /**
     * 更新上下文缓存（追加最新一轮对话）
     */
    void updateContext(Long sessionId, String userMessage, String reply, String imgUrl);
}