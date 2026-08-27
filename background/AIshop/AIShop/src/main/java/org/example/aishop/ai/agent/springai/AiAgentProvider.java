package org.example.aishop.ai.agent.springai;

import org.example.aishop.dto.ai.ChatRequestV2;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Agent 统一入口接口
 *
 * 新旧模块（LangChain4j / Spring AI）均实现此接口，
 * Controller 通过此接口调用，对具体实现无感知。
 */
public interface AiAgentProvider {

    /**
     * 处理 AI 对话请求，返回 SSE 流式连接
     *
     * @param userId  当前用户 ID
     * @param request 请求参数
     * @return SSE 连接
     */
    SseEmitter onRequest(Long userId, ChatRequestV2 request);
}