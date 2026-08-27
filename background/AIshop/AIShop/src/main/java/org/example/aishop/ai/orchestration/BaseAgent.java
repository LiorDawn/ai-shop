package org.example.aishop.ai.orchestration;

import cn.hutool.json.JSONObject;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.example.aishop.dto.ai.ChatRequestV2;
import org.example.aishop.dto.ai.ToolCallResult;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 顶层抽象父类 — 模板方法模式，管控整体 AI 对话生命周期
 *
 * <h3>模板骨架（final，子类不可重写）</h3>
 * <pre>
 *  onRequest()                          ← 入口：组件校验 + 初始化 SSE + 异步执行
 *    └─ run()                           ← 模板方法（final）
 *        ├─ validateComponents()        ← 空指针兜底校验
 *        ├─ onBeforeRun()               ← Hook
 *        ├─ memory.loadHistory()        ← 加载历史上下文
 *        ├─ planner.classifyIntent()    ← 意图前置判定
 *        ├─ onBeforeToolLoop()          ← Hook
 *        ├─ while (toolsLoop)           ← 工具调用循环
 *        │   ├─ planner.plan()          ← 工具决策
 *        │   ├─ circuitBreakers()       ← 熔断检查
 *        │   ├─ onBeforeToolExecute()   ← Hook
 *        │   └─ toolExecutor.execute()  ← 执行工具
 *        │   └─ onAfterToolExecute()    ← Hook
 *        ├─ onAfterToolLoop()           ← Hook
 *        └─ summarizer.streamReply()    ← 流式输出 + 会话落地
 *        └─ onAfterRun()                ← Hook
 * </pre>
 *
 * <h3>子类必须实现</h3>
 * <pre>
 *  abstract Memory       getMemory();
 *  abstract Planner      getPlanner();
 *  abstract ToolExecutor getToolExecutor();
 *  abstract Summarizer   getSummarizer();
 *  abstract Executor     getExecutor();
 * </pre>
 *
 * <h3>子类可选覆盖（Hook 扩展点）</h3>
 * <pre>
 *  onBeforeRun()          onAfterRun()
 *  onBeforeToolLoop()     onAfterToolLoop()
 *  onBeforeToolExecute()  onAfterToolExecute()
 *  onError()
 * </pre>
 */
public abstract class BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    // ================== 熔断常量 ==================

    /** 最大工具调用轮次 */
    protected int maxToolLoops() { return 5; }
    /** 同一工具连续调用上限 */
    protected int maxSameToolRepeat() { return 3; }
    /** 上下文长度熔断阈值（字符数） */
    protected int maxContextChars() { return 8000; }

    // ================== 组件校验 ==================

    /**
     * 组件空指针兜底校验（在 run() 前调用）
     *
     * 子类通过 @Autowired 注入四层组件，如果忘记注入或 Bean 缺失，
     * 此处会抛出明确的 IllegalStateException，避免 NPE 在流程深处才暴露。
     */
    protected void validateComponents() {
        Objects.requireNonNull(getMemory(), "Memory 组件未注入，请检查子类 @Autowired");
        Objects.requireNonNull(getPlanner(), "Planner 组件未注入，请检查子类 @Autowired");
        Objects.requireNonNull(getToolExecutor(), "ToolExecutor 组件未注入，请检查子类 @Autowired");
        Objects.requireNonNull(getSummarizer(), "Summarizer 组件未注入，请检查子类 @Autowired");
        Objects.requireNonNull(getExecutor(), "Executor 组件未注入，请检查子类 @Autowired");
        log.debug("组件校验通过: Memory={}, Planner={}, ToolExecutor={}, Summarizer={}",
                getMemory().getClass().getSimpleName(),
                getPlanner().getClass().getSimpleName(),
                getToolExecutor().getClass().getSimpleName(),
                getSummarizer().getClass().getSimpleName());
    }

    // ================== 模板方法：入口 ==================

    /**
     * SSE 入口 — 子类直接调用
     *
     * @param userId  当前用户 ID
     * @param request 请求参数
     * @return SSE 连接
     */
    public final SseEmitter onRequest(Long userId, ChatRequestV2 request) {
        // 组件空指针兜底校验
        try {
            validateComponents();
        } catch (Exception e) {
            log.error("组件校验失败: {}", e.getMessage());
            SseEmitter failEmitter = new SseEmitter();
            failEmitter.completeWithError(e);
            return failEmitter;
        }

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
                // 打印 HTTP 状态码（如果是 OpenAiHttpException）
                if (e instanceof OpenAiHttpException oe) {
                    log.error("远程接口 HTTP 错误: code={}, message={}", oe.code(), oe.getMessage());
                } else if (e.getCause() instanceof OpenAiHttpException oe) {
                    log.error("远程接口 HTTP 错误(嵌套): code={}, message={}", oe.code(), oe.getMessage());
                }
                onError(userId, sessionId, e);
                if (!disconnected[0]) {
                    sendAndComplete(emitter, "msg", "抱歉，系统出现了一些问题，请稍后再试。");
                    sendAndComplete(emitter, "done", "");
                }
            }
        }, getExecutor());

        return emitter;
    }

    // ================== 模板方法：核心流程（final） ==================

    protected final void run(Long userId, Long sessionId, ChatRequestV2 request,
                             SseEmitter emitter, boolean[] disconnected) {
        long startTime = System.currentTimeMillis();

        Memory memory = getMemory();
        Planner planner = getPlanner();
        ToolExecutor toolExecutor = getToolExecutor();
        Summarizer summarizer = getSummarizer();

        String userMessage = request.getMessage();
        List<ToolCallResult> toolResults = new ArrayList<>();
        List<ChatMessage> messages = new ArrayList<>();

        // Hook: 运行前
        onBeforeRun(userId, sessionId, request);

        // ====== Step 0: 意图前置判定 ======
        boolean toolEnabled = request.getToolEnabled() != null && request.getToolEnabled();
        boolean needsTool = toolEnabled && planner.classifyIntent(userMessage);
        if (!needsTool && toolEnabled) {
            log.info("无意图判定：跳过工具调用 — \"{}\"", userMessage);
        }

        // ====== Step 1: 构建上下文 ======
        messages.add(new SystemMessage(memory.buildSystemPrompt(userId, request, needsTool)));
        messages.addAll(memory.loadHistory(sessionId));
        messages.add(new UserMessage(userMessage));

        // ====== Step 2-5: 工具调用循环 ======
        if (needsTool && !disconnected[0]) {
            // Hook: 工具循环前
            onBeforeToolLoop(userId, sessionId, messages);

            AtomicInteger loopCount = new AtomicInteger(0);
            String lastToolName = null;
            int sameToolRepeatCount = 0;

            while (loopCount.get() < maxToolLoops()) {
                // 断连检测
                if (disconnected[0]) break;

                // 上下文长度熔断
                if (memory.estimateContextLength(messages) > maxContextChars()) {
                    log.warn("上下文长度熔断，终止工具调用循环");
                    break;
                }

                loopCount.incrementAndGet();

                // Step 2-3: 规划 → 工具决策
                Planner.Decision decision = planner.plan(messages);
                if (!decision.valid()) {
                    log.warn("规划器返回无效决策，终止工具调用");
                    break;
                }
                if ("none".equals(decision.toolName())) {
                    log.info("LLM 判定无需工具调用");
                    break;
                }

                // 同工具重复雪崩防护
                if (decision.toolName().equals(lastToolName)) {
                    sameToolRepeatCount++;
                    if (sameToolRepeatCount >= maxSameToolRepeat()) {
                        log.warn("同工具 {} 重复 {} 次，强制终止", decision.toolName(), sameToolRepeatCount);
                        ToolCallResult failResult = ToolCallResult.failure(decision.toolName(),
                                "操作过于频繁，请稍后再试", "REPEAT_LIMIT", 0);
                        toolResults.add(failResult);
                        onAfterToolExecute(userId, sessionId, failResult);
                        break;
                    }
                } else {
                    sameToolRepeatCount = 0;
                    lastToolName = decision.toolName();
                }

                // Hook: 工具执行前
                onBeforeToolExecute(userId, sessionId, decision.toolName(), decision.parameters());

                // Step 4: 执行工具
                ToolCallResult result = toolExecutor.execute(decision.toolName(), decision.parameters());
                toolResults.add(result);

                // Hook: 工具执行后
                onAfterToolExecute(userId, sessionId, result);

                String summary = toolExecutor.summarizeResult(result);
                messages.add(new dev.langchain4j.data.message.AiMessage(summary));

                // Step 5: 终止条件
                if (!decision.needContinue()) break;
            }

            // Hook: 工具循环后
            onAfterToolLoop(userId, sessionId, toolResults);
        }

        // ====== Step 6: 流式输出 + 落盘 ======
        if (disconnected[0]) {
            onAfterRun(userId, sessionId, System.currentTimeMillis() - startTime, toolResults);
            return;
        }

        // 注入 sessionId 到 summarizer（用于持久化时关联会话）
        final Long finalSessionId = sessionId;
        summariesToSession(finalSessionId);

        summarizer.streamReply(sessionId, messages, userMessage, toolResults, emitter, disconnected);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Agent 执行完成: sessionId={}, tools={}, elapsed={}ms", sessionId, toolResults.size(), elapsed);

        // Hook: 运行后
        onAfterRun(userId, sessionId, elapsed, toolResults);
    }

    // ================== 子类必须实现 ==================

    /** 获取记忆层组件 */
    protected abstract Memory getMemory();
    /** 获取规划层组件 */
    protected abstract Planner getPlanner();
    /** 获取工具执行层组件 */
    protected abstract ToolExecutor getToolExecutor();
    /** 获取输出层组件 */
    protected abstract Summarizer getSummarizer();
    /** 获取异步执行器 */
    protected abstract Executor getExecutor();

    // ================== Hook 扩展点（子类可选覆盖） ==================

    /**
     * Hook: 在核心流程开始前调用
     * 可用于：权限校验、限流、日志记录、Metrics 打点
     */
    protected void onBeforeRun(Long userId, Long sessionId, ChatRequestV2 request) {}

    /**
     * Hook: 在核心流程结束后调用（无论成功或失败）
     * 可用于：Metrics 上报、审计日志、资源清理
     */
    protected void onAfterRun(Long userId, Long sessionId, long elapsedMs, List<ToolCallResult> toolResults) {}

    /**
     * Hook: 在工具调用循环开始前调用
     * 可用于：初始化循环级别计数器、预热缓存
     */
    protected void onBeforeToolLoop(Long userId, Long sessionId, List<ChatMessage> messages) {}

    /**
     * Hook: 在工具调用循环结束后调用
     * 可用于：统计工具调用次数和成功率
     */
    protected void onAfterToolLoop(Long userId, Long sessionId, List<ToolCallResult> toolResults) {}

    /**
     * Hook: 在每个工具执行前调用
     * 可用于：参数审计、调用频率检查、灰度控制
     */
    protected void onBeforeToolExecute(Long userId, Long sessionId, String toolName, JSONObject params) {}

    /**
     * Hook: 在每个工具执行后调用
     * 可用于：结果缓存、调用链追踪、异常告警
     */
    protected void onAfterToolExecute(Long userId, Long sessionId, ToolCallResult result) {}

    /**
     * Hook: 在 run() 捕获到未处理异常时调用
     * 可用于：告警推送、错误日志持久化
     */
    protected void onError(Long userId, Long sessionId, Exception e) {}

    // ================== 钩子 & 辅助 ==================

    /**
     * 钩子：将 sessionId 注入到 Summarizer（持久化时需要）
     * 子类可选覆盖
     */
    protected void summariesToSession(Long sessionId) {
        // 默认空实现，子类按需覆盖
    }

    protected Long getOrCreateSession(Long userId, Long sessionId) {
        if (sessionId != null) return sessionId;
        return null;
    }

    protected void sendAndComplete(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
            if ("done".equals(event)) emitter.complete();
        } catch (Exception ignored) {
            // 前端已断开
        }
    }
}