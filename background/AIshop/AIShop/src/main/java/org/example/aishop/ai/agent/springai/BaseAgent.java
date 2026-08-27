package org.example.aishop.ai.agent.springai;

import org.example.aishop.dto.ai.ChatRequestV2;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 第1层：流程引擎｜骨架层
 *
 * <h3>职责</h3>
 * 只管"整个 Agent 怎么跑"，完全不管思考、工具、大模型细节。
 *
 * <h3>核心能力</h3>
 * <pre>
 * - AgentState 状态机：IDLE → RUNNING → FINISHED / ERROR
 * - final run()：参数校验 → while(stepCount < maxSteps) step() → 清理
 * - messageList：统一消息上下文容器，所有子层共用
 * - SSE 封装：sendProcessEvent() / sendTextEvent() / sendFinishEvent()
 * - 全局安全锁 maxSteps（防止死循环）
 * </pre>
 *
 * <h3>抽象方法（子类实现）</h3>
 * <pre>
 * step()：每一步具体做什么，完全由子类决定
 * </pre>
 */
public abstract class BaseAgent implements AiAgentProvider {

    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    // ================== 状态机 ==================

    public enum AgentState {
        IDLE, RUNNING, FINISHED, ERROR
    }

    protected AgentState agentState = AgentState.IDLE;

    // ================== 消息上下文容器 ==================

    /** 所有子层共用同一份对话消息，父类统一维护 */
    protected final List<Message> messageList = new ArrayList<>();

    // ================== 安全锁 ==================

    protected int stepCount = 0;

    /** 子类可覆盖，默认 15 步 */
    protected int maxSteps() { return 15; }

    // ================== 模板方法：入口 ==================

    @Override
    public final SseEmitter onRequest(Long userId, ChatRequestV2 request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        Long sessionId = getOrCreateSession(userId, request.getSessionId());

        // 发送 sessionId
        try {
            emitter.send(SseEmitter.event().name("session").data(sessionId));
        } catch (Exception e) {
            return emitter;
        }

        // SSE 断连回调
        final boolean[] disconnected = {false};
        emitter.onCompletion(() -> disconnected[0] = true);
        emitter.onTimeout(() -> disconnected[0] = true);
        emitter.onError(e -> disconnected[0] = true);

        UserDTO currentUser = UserHolder.getUser();

        CompletableFuture.runAsync(() -> {
            if (currentUser != null) UserHolder.saveUser(currentUser);
            try {
                run(userId, sessionId, request, emitter, disconnected);
            } catch (Exception e) {
                log.error("Agent 执行异常", e);
                agentState = AgentState.ERROR;
                onError(userId, sessionId, e);
                if (!disconnected[0]) {
                    sendTextEvent(emitter, "抱歉，系统出现了一些问题，请稍后再试。");
                    sendFinishEvent(emitter);
                }
            }
        }, getExecutor());

        return emitter;
    }

    // ================== 模板方法：核心循环（final） ==================

    protected final void run(Long userId, Long sessionId, ChatRequestV2 req,
                             SseEmitter emitter, boolean[] disconnected) {

        // 参数校验
        if (req == null || req.getMessage() == null || req.getMessage().trim().isEmpty()) {
            sendTextEvent(emitter, "请输入您的问题。");
            sendFinishEvent(emitter);
            return;
        }

        // 状态切换
        agentState = AgentState.RUNNING;
        stepCount = 0;
        messageList.clear();

        // Hook: 运行前
        onStart(userId, sessionId, req);

        try {
            // 主循环：每步由子类实现
            while (agentState == AgentState.RUNNING && stepCount < maxSteps()) {
                // 断连检测
                if (disconnected[0]) {
                    log.info("SSE 已断开，提前终止，当前步数={}", stepCount);
                    break;
                }

                stepCount++;
                step(userId, sessionId, req, emitter, disconnected);
            }

            // 步数超限安全终止
            if (stepCount >= maxSteps() && agentState == AgentState.RUNNING) {
                log.warn("步数超限，强制终止，maxSteps={}", maxSteps());
                agentState = AgentState.FINISHED;
            }

        } catch (Exception e) {
            log.error("Agent 执行异常", e);
            agentState = AgentState.ERROR;
            onError(userId, sessionId, e);
        }

        // 清理
        onFinish(userId, sessionId);
    }

    // ================== 抽象方法：每一步做什么 ==================

    /**
     * 每一步具体业务逻辑，完全由子类实现。
     * 父类只控制循环调度，不关心 step 内部做什么。
     */
    protected abstract void step(Long userId, Long sessionId, ChatRequestV2 req,
                                  SseEmitter emitter, boolean[] disconnected);

    // ================== 子类必须实现 ==================

    protected abstract Executor getExecutor();
    protected abstract Long getOrCreateSession(Long userId, Long sessionId);

    /**
     * 会话持久化：将本轮对话写入 MySQL + Redis。
     * 在 BaseAgent 定义签名，由子类实现。调用时机由子类决定（通常为流式输出完成后）。
     * 为什么不放在 onFinish() 里自动调用？因为流式输出是异步的（Flux），
     * onFinish() 在 while 循环结束后立即执行，此时流式尚未完成，数据不完整。
     */
    protected abstract void persistConversation(String finalReply);

    // ================== Hook 扩展点 ==================

    protected void onStart(Long userId, Long sessionId, ChatRequestV2 req) {}
    protected void onFinish(Long userId, Long sessionId) {}
    protected void onError(Long userId, Long sessionId, Exception e) {}

    // ================== SSE 推送封装 ==================

    /** 推送过程事件（中间步骤通知前端） */
    protected void sendProcessEvent(SseEmitter emitter, String type, String data) {
        try {
            emitter.send(SseEmitter.event().name("process").data(type + ":" + data));
        } catch (Exception ignored) {}
    }

    /** 推送文本内容（流式 token 或最终文本） */
    protected void sendTextEvent(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().name("msg").data(text));
        } catch (Exception ignored) {}
    }

    /** 推送结束事件 */
    protected void sendFinishEvent(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data(""));
            emitter.complete();
        } catch (Exception ignored) {}
    }

    // ================== 内部类 ==================

    public record Message(String role, String content) {
        public static Message system(String content) { return new Message("system", content); }
        public static Message user(String content) { return new Message("user", content); }
        public static Message ai(String content) { return new Message("assistant", content); }
        public static Message tool(String content) { return new Message("tool", content); }
    }
}