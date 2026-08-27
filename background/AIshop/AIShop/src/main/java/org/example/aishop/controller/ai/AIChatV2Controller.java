package org.example.aishop.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.dto.ai.ChatRequestV2;
import org.example.aishop.ai.agent.springai.AiAgentProvider;
import org.example.aishop.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 智能对话 V2 控制器（接入层）
 *
 * 提供 SSE 流式端点，接收用户请求并委托给 AI 核心编排层处理。
 * 支持 RAG 商品检索增强 + MCP 工具调用（购物车增删改查）。
 */
@Tag(name = "AI 智能对话 V2", description = "基于 Spring AI + MCP 工具调用 + JSON Schema 强制结构化的智能购物助手")
@RestController
@RequestMapping("/ai/v2")
public class AIChatV2Controller {

    private static final Logger log = LoggerFactory.getLogger(AIChatV2Controller.class);

    @Autowired
    private AiAgentProvider agent;

    /**
     * SSE 流式问答端点
     *
     * 前端通过 EventSource 连接，实时接收 AI 逐字回复。
     * 内部走 6 步流程：消息构建 → 首轮 LLM 调用（工具决策）→ 校验 → MCP 执行 → 循环判断 → 二次 LLM 生成回复。
     */
    @Operation(summary = "SSE 流式智能对话", description = "支持 RAG 检索增强 + MCP 购物车工具调用")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) String imgUrl,
            @RequestParam(required = false, defaultValue = "true") Boolean ragEnabled,
            @RequestParam(required = false, defaultValue = "true") Boolean toolEnabled) {

        Long userId = UserHolder.getUserId();

        ChatRequestV2 request = new ChatRequestV2();
        request.setSessionId(sessionId);
        request.setMessage(message);
        request.setImgUrl(imgUrl);
        request.setRagEnabled(ragEnabled);
        request.setToolEnabled(toolEnabled);

        log.info("SSE V2 请求: userId={}, message={}, ragEnabled={}, toolEnabled={}",
                userId, message, ragEnabled, toolEnabled);

        return agent.onRequest(userId, request);
    }
}