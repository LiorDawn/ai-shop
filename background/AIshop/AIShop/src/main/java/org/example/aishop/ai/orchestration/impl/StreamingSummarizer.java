package org.example.aishop.ai.orchestration.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.example.aishop.dto.ai.ToolCallResult;
import org.example.aishop.ai.orchestration.Summarizer;
import org.example.aishop.ai.storage.mysql.MysqlPersistService;
import org.example.aishop.ai.storage.redis.RedisConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 流式输出器 — LLM 汇总 + SSE 流式推送 + 会话落地
 */
@Component
public class StreamingSummarizer implements Summarizer {

    private static final Logger log = LoggerFactory.getLogger(StreamingSummarizer.class);

    @Autowired
    private StreamingChatLanguageModel streamingChatLanguageModel;

    @Autowired
    private MysqlPersistService mysqlPersistService;

    @Autowired
    private RedisConversationService redisConversationService;

    @Override
    public void streamReply(Long sessionId, List<ChatMessage> messages, String userMessage,
                            List<ToolCallResult> toolResults, SseEmitter emitter, boolean[] disconnected) {
        String summaryPrompt = buildSummaryPrompt(userMessage, toolResults);
        messages.add(new UserMessage(summaryPrompt));

        StringBuilder fullReply = new StringBuilder();
        final Long sid = sessionId;

        try {
            streamingChatLanguageModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    if (token != null && !disconnected[0]) {
                        fullReply.append(token);
                        try {
                            emitter.send(SseEmitter.event().name("msg").data(token));
                        } catch (Exception e) {
                            disconnected[0] = true;
                        }
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    String reply = fullReply.toString();
                    // 先关闭 SSE 通道：发送 done + complete。
                    // 若把持久化放在 complete 之前，DB 慢/异常会中断此方法，
                    // 导致 done/complete 永不发送，连接空挂到 120s 超时。
                    if (!disconnected[0]) {
                        sendFinish(emitter);
                    }
                    // 再持久化：失败不影响流式响应结束
                    if (!reply.isEmpty()) {
                        try {
                            persistConversation(sid, userMessage, reply, null, toolResults);
                        } catch (Exception e) {
                            log.error("会话持久化失败", e);
                        }
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式生成失败", error);
                    if (!disconnected[0]) {
                        String fallback = buildFallback(userMessage, toolResults);
                        sendAndFinish(emitter, fallback, disconnected);
                    }
                }
            });
        } catch (Exception e) {
            log.error("流式生成异常", e);
            if (!disconnected[0]) {
                String fallback = buildFallback(userMessage, toolResults);
                sendAndFinish(emitter, fallback, disconnected);
            }
        }
    }

    @Override
    public void persistConversation(Long sessionId, String userMessage, String reply,
                                    String imgUrl, List<ToolCallResult> toolResults) {
        if (sessionId == null) return;

        // 用户消息
        mysqlPersistService.saveMessage(sessionId, "user", userMessage, imgUrl);

        // 工具调用记录
        if (toolResults != null && !toolResults.isEmpty()) {
            StringBuilder toolLog = new StringBuilder();
            for (ToolCallResult r : toolResults) {
                toolLog.append("[").append(r.getToolName()).append("] ")
                        .append(r.isSuccess() ? "成功" : "失败")
                        .append(": ").append(r.getMessage()).append("\n");
            }
            mysqlPersistService.saveMessage(sessionId, "system", toolLog.toString(), null);
        }

        // AI 回复
        mysqlPersistService.saveMessage(sessionId, "assistant", reply, null);

        // 更新会话时间
        mysqlPersistService.updateSessionLastTime(sessionId);

        // Redis 热数据缓存
        redisConversationService.updateContext(sessionId, userMessage, reply, imgUrl);
    }

    @Override
    public String buildFallback(String userMessage, List<ToolCallResult> toolResults) {
        if (toolResults != null && !toolResults.isEmpty()) {
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

    private String buildSummaryPrompt(String userMessage, List<ToolCallResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return userMessage;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【系统指令】以下是用户刚才请求的工具执行结果，请整合这些结果，用自然、友好的语言向用户汇报操作结果。\n\n");
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
            sb.append("\n⚠️ 有 ").append(failCount).append(" 个操作失败，请在回复中诚实告知用户失败原因，");
            sb.append("并建议用户通过正常页面操作或联系客服。");
        }

        sb.append("\n请生成一段简洁的回复（不超过 200 字），告诉用户操作结果。");
        return sb.toString();
    }

    private void sendAndFinish(SseEmitter emitter, String text, boolean[] disconnected) {
        try {
            emitter.send(SseEmitter.event().name("msg").data(text));
            sendFinish(emitter);
        } catch (Exception ignored) {
            disconnected[0] = true;
        }
    }

    private void sendFinish(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data(""));
            emitter.complete();
        } catch (Exception ignored) {
            // 前端已断开
        }
    }
}