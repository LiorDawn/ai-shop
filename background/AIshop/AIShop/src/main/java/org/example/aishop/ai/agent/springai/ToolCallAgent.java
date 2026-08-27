package org.example.aishop.ai.agent.springai;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolRegistry;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.ai.rag.RagService;
import org.example.aishop.ai.storage.mysql.MysqlPersistService;
import org.example.aishop.ai.storage.redis.RedisConversationService;
import org.example.aishop.dto.ai.ChatRequestV2;
import org.example.aishop.dto.ai.ToolCallResult;
import org.example.aishop.entity.ai.AIChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 第3层：业务实现层｜整个体系的核心业务层
 *
 * <h3>职责</h3>
 * 完整实现 think() + act()，落地工具调用全链路逻辑。
 * 上层所有抽象方法在这里全部填完，代码量最大（~600行）。
 *
 * <h3>think() — 思考推理</h3>
 * <pre>
 * 首次调用：加载历史 → 意图分类 → RAG检索 → 拼装Prompt → 调大模型 → 解析工具决策
 * 后续调用：调大模型（含工具结果上下文）→ 解析工具决策
 * </pre>
 *
 * <h3>act() — 执行行动</h3>
 * <pre>
 * 有工具决策：执行工具 → 结果回写 messageList → 判断是否继续
 * 终止决策：流式生成回复 → 会话落盘 → 状态设为 FINISHED
 * </pre>
 *
 * <h3>继承关系</h3>
 * 后续新建不同业务 Agent，直接继承本类即可，不需要重写 think/act 整套逻辑。
 */
public abstract class ToolCallAgent extends ReActAgent {

    private static final Logger log = LoggerFactory.getLogger(ToolCallAgent.class);
    private static final String RAG_CACHE_PREFIX = "AISHOP:AI:RAG_CACHE:";
    private static final int MAX_RESULT_CHARS = 500;
    private static final int MAX_SAME_TOOL_REPEAT = 3;
    private static final int MAX_CONTEXT_CHARS = 8000;

    /** 纯闲聊关键词，命中即跳过工具调用 */
    private static final Set<String> CHAT_KEYWORDS = Set.of(
            "你好", "嗨", "hi", "hello", "谢谢", "再见", "拜拜", "你是谁",
            "天气", "今天", "什么是", "为什么", "怎么样", "好不好",
            "讲个笑话", "故事", "新闻", "你会什么", "能做什么",
            "天气怎么样", "时间", "日期", "放假", "周末"
    );

    // ================== 注入依赖 ==================

    @Autowired
    protected ChatClient chatClient;
    @Autowired
    protected RagService ragService;
    @Autowired
    protected RedisConversationService redisConversationService;
    @Autowired
    protected MysqlPersistService mysqlPersistService;
    @Autowired
    protected StringRedisTemplate stringRedisTemplate;
    @Autowired
    private McpToolRegistry mcpToolRegistry;

    // ================== 内部状态 ==================

    /** 是否已完成首次初始化（历史加载、意图分类、RAG检索、Prompt构建） */
    private boolean initialized = false;

    /** 是否应终止并生成回复 */
    private boolean shouldTerminate = false;

    /** 本轮工具决策 */
    private ToolDecision currentDecision = null;

    /** 工具调用结果列表 */
    private final List<ToolCallResult> toolResults = new ArrayList<>();

    /** 工具重复计数 */
    private String lastToolName = null;
    private int sameToolRepeatCount = 0;

    /** 会话ID */
    private Long currentSessionId;

    /** 用户原始消息 */
    private String userMessage;

    // ================== 重置状态（每次新请求调用） ==================

    @Override
    protected void onStart(Long userId, Long sessionId, ChatRequestV2 req) {
        // ★ 修复：每次新请求重置所有状态，防止单例 Bean 状态污染
        initialized = false;
        shouldTerminate = false;
        currentDecision = null;
        toolResults.clear();
        lastToolName = null;
        sameToolRepeatCount = 0;
    }

    // ================== think() — 思考推理 ==================

    @Override
    protected boolean think(Long userId, Long sessionId, ChatRequestV2 req,
                            SseEmitter emitter, boolean[] disconnected) {
        currentSessionId = sessionId;
        userMessage = req.getMessage();

        try {
            // 首次调用：完整初始化
            if (!initialized) {
                initialize(userId, req);
                initialized = true;
            }

            // 上下文长度熔断
            if (estimateContextLength() > MAX_CONTEXT_CHARS) {
                log.warn("上下文长度熔断，终止工具调用");
                shouldTerminate = true;
                return false;
            }

            // 调用大模型，获取工具决策
            String llmResponse = callLLM();
            currentDecision = parseDecision(llmResponse);

            // 判断是否需要执行工具
            if (currentDecision == null || !currentDecision.valid()) {
                log.warn("大模型返回无效决策，终止");
                shouldTerminate = true;
                return false;
            }

            if ("stop".equals(currentDecision.toolName()) || "none".equals(currentDecision.toolName())) {
                log.info("大模型返回 {}，终止工具调用", currentDecision.toolName());
                shouldTerminate = true;
                return false;
            }

            // 同工具重复熔断
            if (currentDecision.toolName().equals(lastToolName)) {
                sameToolRepeatCount++;
                if (sameToolRepeatCount >= MAX_SAME_TOOL_REPEAT) {
                    log.warn("同工具 {} 重复 {} 次，强制终止", currentDecision.toolName(), sameToolRepeatCount);
                    toolResults.add(ToolCallResult.failure(currentDecision.toolName(),
                            "操作过于频繁，请稍后再试", "REPEAT_LIMIT", 0));
                    shouldTerminate = true;
                    return false;
                }
            } else {
                sameToolRepeatCount = 0;
                lastToolName = currentDecision.toolName();
            }

            return true;  // 需要执行工具

        } catch (Exception e) {
            log.error("think 阶段异常", e);
            shouldTerminate = true;
            return false;
        }
    }

    // ================== act() — 执行行动 ==================

    @Override
    protected void act(Long userId, Long sessionId, ChatRequestV2 req,
                       SseEmitter emitter, boolean[] disconnected) {

        if (shouldTerminate) {
            // 终止：流式生成回复（think 阶段已判定 stop/none）
            sendProcessEvent(emitter, "thinking", "正在生成回复");
            streamReply(emitter, disconnected);
            agentState = AgentState.FINISHED;
            return;
        }

        if (currentDecision == null) {
            agentState = AgentState.FINISHED;
            return;
        }

        try {
            // 执行工具
            sendProcessEvent(emitter, "tool", currentDecision.toolName());
            ToolCallResult result = mcpToolRegistry.execute(currentDecision.toolName(), currentDecision.parameters());
            toolResults.add(result);

            // 工具结果回写 messageList
            String summary = summarizeResult(result);
            messageList.add(Message.tool(summary));
            log.info("工具执行完成: {} → {}", currentDecision.toolName(), result.isSuccess() ? "成功" : "失败");

            // ★ 大模型说不需要继续了 → 直接流式输出，不等下一轮
            if (!currentDecision.needContinue()) {
                shouldTerminate = true;
                sendProcessEvent(emitter, "thinking", "正在生成回复");
                streamReply(emitter, disconnected);
                agentState = AgentState.FINISHED;
                return;
            }

        } catch (Exception e) {
            log.error("act 阶段异常", e);
            shouldTerminate = true;
            sendProcessEvent(emitter, "thinking", "正在生成回复");
            streamReply(emitter, disconnected);
            agentState = AgentState.FINISHED;
        }
    }

    // ================== 初始化（首次调用） ==================

    private void initialize(Long userId, ChatRequestV2 req) {
        long initStart = System.currentTimeMillis();
        // ① 加载历史消息
        List<Message> history = loadHistory(currentSessionId);
        messageList.addAll(history);

        // ② 意图分类
        boolean toolEnabled = req.getToolEnabled() != null && req.getToolEnabled();
        boolean needsTool = toolEnabled && classifyIntent(userMessage);

        // ③ RAG 检索（仅导购意图）
        String ragContext = "";
        if (needsTool && req.getRagEnabled() != null && req.getRagEnabled()) {
            ragContext = retrieveRagContext(userMessage);
            if (!ragContext.isEmpty()) {
                sendProcessEvent(null, "rag", "已检索到相关商品信息");
            }
        }

        // ④ 构建系统提示词
        String systemPrompt = buildSystemPrompt(needsTool, ragContext);
        messageList.add(0, Message.system(systemPrompt));
        messageList.add(Message.user(userMessage));

        log.info("⏱️ 初始化完成: needsTool={}, ragLength={}, historySize={}, 耗时: {}ms",
                needsTool, ragContext.length(), history.size(), System.currentTimeMillis() - initStart);
    }

    // ================== 大模型调用 ==================

    private String callLLM() {
        List<org.springframework.ai.chat.messages.Message> aiMessages = toSpringAiMessages(messageList);
        long start = System.currentTimeMillis();
        // ★ 工具决策只需短 JSON，maxTokens=256 大幅加速（原 2000）
        String result = chatClient.prompt()
                .messages(aiMessages)
                .options(org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .maxTokens(256)
                        .build())
                .call()
                .content();
        log.info("⏱️ LLM 工具决策耗时: {}ms, 返回长度: {} 字符", System.currentTimeMillis() - start, result.length());
        return result;
    }

    private ToolDecision parseDecision(String llmOutput) {
        try {
            String json = llmOutput.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }
            JSONObject obj = new JSONObject(json);
            String toolName = obj.getStr("toolName", "stop");
            JSONObject params = obj.getJSONObject("parameters");
            if (params == null) params = new JSONObject();
            boolean needContinue = obj.getBool("needContinueTool", false);
            return new ToolDecision(toolName, params, needContinue, true);
        } catch (Exception e) {
            log.warn("工具决策 JSON 解析失败: {}", llmOutput);
            return null;
        }
    }

    // ================== 流式生成回复 ==================

    private void streamReply(SseEmitter emitter, boolean[] disconnected) {
        // ★ fix: 构建副本，不修改原始 messageList，避免每轮膨胀
        String summaryPrompt = buildSummaryPrompt();
        List<Message> streamMessages = new ArrayList<>(messageList);

        // ★ 流式回复阶段：用最简提示词替换工具决策提示词，减少 token 加速响应
        if (!streamMessages.isEmpty() && "system".equals(streamMessages.get(0).role())) {
            streamMessages.set(0, Message.system(getSystemPrompt() + "\n\n请用简洁自然的语言回复用户，不要输出 JSON 格式。"));
        }
        streamMessages.add(Message.user(summaryPrompt));

        List<org.springframework.ai.chat.messages.Message> aiMessages = toSpringAiMessages(streamMessages);
        StringBuilder fullReply = new StringBuilder();

        chatClient.prompt()
                .messages(aiMessages)
                .stream()
                .content()
                .subscribe(
                        token -> {
                            if (token != null && !disconnected[0]) {
                                fullReply.append(token);
                                sendTextEvent(emitter, token);
                            }
                        },
                        error -> {
                            log.error("流式生成失败", error);
                            if (!disconnected[0]) {
                                String fallback = buildFallback();
                                sendTextEvent(emitter, fallback);
                            }
                            // ★ fix: 断连或失败时也落盘，不丢失会话
                            persistConversation(fullReply.toString());
                            if (!disconnected[0]) {
                                sendFinishEvent(emitter);
                            }
                        },
                        () -> {
                            // onComplete: 会话落盘
                            String reply = fullReply.toString();
                            persistConversation(reply);
                            if (!disconnected[0]) {
                                sendFinishEvent(emitter);
                            }
                        }
                );
    }

    // ================== 历史消息 ==================

    private List<Message> loadHistory(Long sessionId) {
        if (sessionId == null) return Collections.emptyList();
        List<AIChatMessage> dbMessages = mysqlPersistService.getRecentMessages(sessionId, 20);
        if (dbMessages == null || dbMessages.isEmpty()) return Collections.emptyList();
        List<AIChatMessage> reversed = new ArrayList<>(dbMessages);
        Collections.reverse(reversed);
        return reversed.stream()
                .map(m -> new Message(m.getRole(), m.getContent()))
                .collect(Collectors.toList());
    }

    // ================== 意图分类 ==================

    private boolean classifyIntent(String msg) {
        if (msg == null || msg.trim().isEmpty()) return false;
        String trimmed = msg.trim().toLowerCase();
        if (trimmed.length() < 2) return false;
        for (String keyword : CHAT_KEYWORDS) {
            if (trimmed.contains(keyword.toLowerCase())) return false;
        }
        return true;
    }

    // ================== RAG 检索 ==================

    private String retrieveRagContext(String msg) {
        String cacheKey = RAG_CACHE_PREFIX + Integer.toHexString(msg.hashCode());
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            log.debug("RAG 检索命中缓存");
            return cached;
        }
        String ragContext = ragService.buildRagContext(msg);
        if (ragContext != null && !ragContext.isEmpty()) {
            stringRedisTemplate.opsForValue().set(cacheKey, ragContext, 60, TimeUnit.SECONDS);
        }
        return ragContext != null ? ragContext : "";
    }

    // ================== 系统提示词 ==================

    private String buildSystemPrompt(boolean needsTool, String ragContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(getSystemPrompt());  // ← 第4层提供

        if (ragContext != null && !ragContext.isEmpty()) {
            prompt.append("\n\n").append(ragContext);
        }

        if (needsTool) {
            prompt.append("\n\n").append(ToolCallSchema.buildToolSchemaPrompt());
        }

        prompt.append("\n\n").append(getNextStepPrompt());  // ← 第4层提供

        return prompt.toString();
    }

    // ================== 工具结果摘要 ==================

    private String summarizeResult(ToolCallResult result) {
        String text = String.format("[工具执行结果] %s: %s — %s",
                result.getToolName(),
                result.isSuccess() ? "成功" : "失败",
                result.getMessage());
        if (text.length() > MAX_RESULT_CHARS) {
            return text.substring(0, MAX_RESULT_CHARS)
                    + "\n...（结果过长已截断，共 " + text.length() + " 字符）";
        }
        return text;
    }

    // ================== 汇总 Prompt ==================

    private String buildSummaryPrompt() {
        if (toolResults.isEmpty()) {
            return "【系统指令】请回答用户的问题：" + userMessage;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【系统指令】以下是工具执行结果，请整合后回复用户。\n\n");
        sb.append("用户原始问题：").append(userMessage).append("\n\n");
        sb.append("工具执行记录：\n");
        for (int i = 0; i < toolResults.size(); i++) {
            ToolCallResult r = toolResults.get(i);
            sb.append(i + 1).append(". ").append(r.getToolName()).append(": ")
                    .append(r.isSuccess() ? "✅ 成功" : "❌ 失败")
                    .append(" — ").append(r.getMessage()).append("\n");
        }
        long failCount = toolResults.stream().filter(r -> !r.isSuccess()).count();
        if (failCount > 0) {
            sb.append("\n⚠️ 有 ").append(failCount).append(" 个操作失败，请在回复中诚实告知失败原因。");
        }
        sb.append("\n请生成一段简洁的回复（不超过 200 字）。");
        return sb.toString();
    }

    // ================== 兜底回复 ==================

    private String buildFallback() {
        if (!toolResults.isEmpty()) {
            long successCount = toolResults.stream().filter(ToolCallResult::isSuccess).count();
            long failCount = toolResults.size() - successCount;
            StringBuilder sb = new StringBuilder();
            if (successCount > 0) {
                sb.append("✅ 成功执行 ").append(successCount).append(" 个操作：\n");
                toolResults.stream().filter(ToolCallResult::isSuccess)
                        .forEach(r -> sb.append("  - ").append(r.getMessage()).append("\n"));
            }
            if (failCount > 0) {
                sb.append("❌ ").append(failCount).append(" 个操作失败：\n");
                toolResults.stream().filter(r -> !r.isSuccess())
                        .forEach(r -> sb.append("  - ").append(r.getMessage()).append("\n"));
            }
            return sb.toString();
        }
        return "抱歉，我暂时无法处理您的请求。请尝试通过页面菜单操作，或稍后再试。如需帮助，可以联系在线客服。";
    }

    // ================== 会话落盘（实现 BaseAgent 抽象方法） ==================

    @Override
    protected void persistConversation(String reply) {
        if (currentSessionId == null) return;
        mysqlPersistService.saveMessage(currentSessionId, "user", userMessage, null);
        if (!toolResults.isEmpty()) {
            StringBuilder toolLog = new StringBuilder();
            for (ToolCallResult r : toolResults) {
                toolLog.append("[").append(r.getToolName()).append("] ")
                        .append(r.isSuccess() ? "成功" : "失败")
                        .append(": ").append(r.getMessage()).append("\n");
            }
            mysqlPersistService.saveMessage(currentSessionId, "system", toolLog.toString(), null);
        }
        mysqlPersistService.saveMessage(currentSessionId, "assistant", reply, null);
        mysqlPersistService.updateSessionLastTime(currentSessionId);
        redisConversationService.updateContext(currentSessionId, userMessage, reply, null);
    }

    // ================== 辅助方法 ==================

    private int estimateContextLength() {
        int total = 0;
        for (Message msg : messageList) {
            total += msg.content() != null ? msg.content().length() : 0;
        }
        return total;
    }

    private List<org.springframework.ai.chat.messages.Message> toSpringAiMessages(List<Message> messages) {
        List<org.springframework.ai.chat.messages.Message> result = new ArrayList<>();
        for (Message msg : messages) {
            result.add(switch (msg.role()) {
                case "system" -> new org.springframework.ai.chat.messages.SystemMessage(msg.content());
                case "user" -> new org.springframework.ai.chat.messages.UserMessage(msg.content());
                case "tool" -> new org.springframework.ai.chat.messages.AssistantMessage(msg.content());
                default -> new org.springframework.ai.chat.messages.AssistantMessage(msg.content());
            });
        }
        return result;
    }

    // ================== 第4层提供 ==================

    /** 角色系统提示词：定义 AI 身份、人设、业务规则 */
    protected abstract String getSystemPrompt();

    /** 下一步指令提示词：指导模型如何做决策 */
    protected abstract String getNextStepPrompt();

    // ================== 供 ReActAgent 调用 ==================

    /** 供第2层判断是否跳过 think() 直调 act() */
    @Override
    protected boolean shouldTerminate() {
        return shouldTerminate;
    }

    // ================== 内部类 ==================

    public record ToolDecision(String toolName, JSONObject parameters, boolean needContinue, boolean valid) {
        public static ToolDecision invalid() {
            return new ToolDecision(null, null, false, false);
        }
    }
}