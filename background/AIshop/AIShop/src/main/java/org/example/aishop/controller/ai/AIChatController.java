package org.example.aishop.controller.ai;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.dto.ai.ChatRequestV2;
import org.example.aishop.service.ai.AIChatSessionService;
import org.example.aishop.ai.orchestration.AIChatAgent;
import org.example.aishop.common.result.Result;
import org.example.aishop.entity.ai.AIChatMessage;
import org.example.aishop.entity.ai.AISession;
import org.example.aishop.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI 聊天控制器（V1 兼容 + 会话管理）
 *
 * 提供会话 CRUD、历史消息查询、图片上传、V1 流式/非流式对话。
 * V2 流式对话由 AIChatV2Controller 处理。
 */
@Tag(name = "AI 智能对话", description = "AI 会话管理、消息查询、V1 流式对话")
@RestController
@RequestMapping("/chat")
public class AIChatController {

    private static final Logger log = LoggerFactory.getLogger(AIChatController.class);

    @Autowired
    private AIChatSessionService sessionService;

    @Autowired
    private AIChatAgent agent;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    // ==================== 会话管理 ====================

    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    public Result<List<AISession>> getSessions() {
        return Result.success(sessionService.listSessions());
    }

    @Operation(summary = "创建新会话")
    @PostMapping("/session")
    public Result<AISession> createSession() {
        return Result.success(sessionService.createSession());
    }

    @Operation(summary = "重命名会话")
    @PutMapping("/session/{id}/rename")
    public Result<Void> renameSession(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.trim().isEmpty()) {
            return Result.fail("标题不能为空");
        }
        sessionService.renameSession(id, title.trim());
        return Result.success();
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return Result.success();
    }

    @Operation(summary = "获取最近会话")
    @GetMapping("/latest-session")
    public Result<AISession> getLatestSession() {
        AISession session = sessionService.getLatestSession();
        return Result.success(session);
    }

    // ==================== 消息查询 ====================

    @Operation(summary = "获取会话历史消息")
    @GetMapping("/messages/{sessionId}")
    public Result<List<AIChatMessage>> getMessages(@PathVariable Long sessionId) {
        return Result.success(sessionService.getMessages(sessionId));
    }

    @Operation(summary = "获取会话消息（别名）")
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<AIChatMessage>> getSessionMessages(@PathVariable Long sessionId) {
        return Result.success(sessionService.getMessages(sessionId));
    }

    @Operation(summary = "分页获取会话消息（倒序）")
    @GetMapping("/session/{sessionId}/messages/page")
    public Result<?> getSessionMessagesPage(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(sessionService.getMessagesPage(sessionId, page, size));
    }

    // ==================== 图片上传 ====================

    @Operation(summary = "上传 AI 对话图片")
    @PostMapping("/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = sessionService.uploadImage(file);
        return Result.success(url);
    }

    // ==================== V1 非流式对话 ====================

    @Operation(summary = "发送消息获取 AI 回复（非流式，兼容旧接口）")
    @PostMapping("/send")
    public Result<Map<String, Object>> sendMessage(@RequestBody ChatRequestV2 request) {
        Long userId = UserHolder.getUserId();
        Long sessionId = sessionService.getOrCreateSession(request.getSessionId());

        // 保存用户消息
        String userMessage = request.getMessage();
        sessionService.saveUserMessage(sessionId, userMessage, request.getImgUrl());

        // 同步调用 LLM
        String systemPrompt = "你是一个智能购物助手，帮助用户解答购物相关问题。请用简洁友好的中文回复。";
        dev.langchain4j.data.message.ChatMessage sysMsg = SystemMessage.from(systemPrompt);
        dev.langchain4j.data.message.ChatMessage userMsg = UserMessage.from(userMessage);
        String reply = chatLanguageModel.generate(sysMsg, userMsg).content().text();

        // 保存 AI 回复
        sessionService.saveAssistantMessage(sessionId, reply);

        log.info("V1 非流式回复: sessionId={}, replyLen={}", sessionId, reply.length());
        return Result.success(Map.of("sessionId", sessionId, "reply", reply));
    }

    // ==================== V1 SSE 流式对话 ====================

    @Operation(summary = "SSE 流式智能对话（V1 兼容）")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) String imgUrl,
            @RequestParam(required = false, defaultValue = "true") Boolean ragEnabled) {

        Long userId = UserHolder.getUserId();

        // 获取或创建会话
        Long resolvedSessionId = sessionService.getOrCreateSession(sessionId);

        // 保存用户消息
        sessionService.saveUserMessage(resolvedSessionId, message, imgUrl);

        ChatRequestV2 request = new ChatRequestV2();
        request.setSessionId(resolvedSessionId);
        request.setMessage(message);
        request.setImgUrl(imgUrl);
        request.setRagEnabled(ragEnabled);
        request.setToolEnabled(false); // V1 模式不启用工具调用

        log.info("SSE V1 请求: userId={}, message={}, sessionId={}", userId, message, resolvedSessionId);

        return agent.onRequest(userId, request);
    }
}