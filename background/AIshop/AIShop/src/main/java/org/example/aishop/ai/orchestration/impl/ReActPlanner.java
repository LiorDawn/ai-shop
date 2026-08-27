package org.example.aishop.ai.orchestration.impl;

import cn.hutool.json.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.ai4j.openai4j.OpenAiHttpException;
import org.example.aishop.ai.orchestration.ParameterFiller;
import org.example.aishop.ai.orchestration.Planner;
import org.example.aishop.ai.orchestration.SchemaValidator;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.ai.support.RetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * ReAct 规划器 — 意图分类 + 工具决策 + Schema 校验 + 重试
 */
@Component
public class ReActPlanner implements Planner {

    private static final Logger log = LoggerFactory.getLogger(ReActPlanner.class);
    private static final int MAX_SCHEMA_RETRIES = 2;

    /** 纯闲聊/知识问答关键词，命中即跳过工具调用 */
    private static final Set<String> CHAT_KEYWORDS = Set.of(
            "你好", "嗨", "hi", "hello", "谢谢", "再见", "拜拜", "你是谁",
            "天气", "今天", "什么是", "为什么", "怎么样", "好不好",
            "讲个笑话", "故事", "新闻", "你会什么", "能做什么",
            "天气怎么样", "时间", "日期", "放假", "周末"
    );

    @Autowired
    @Qualifier("plannerChatModel")
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private SchemaValidator schemaValidator;

    @Autowired
    private ParameterFiller parameterFiller;

    @Autowired
    private RetryHandler retryHandler;

    @Override
    public boolean classifyIntent(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) return false;
        String trimmed = userMessage.trim().toLowerCase();
        if (trimmed.length() < 2) return false;

        for (String keyword : CHAT_KEYWORDS) {
            if (trimmed.contains(keyword.toLowerCase())) return false;
        }
        return true;
    }

    @Override
    public Decision plan(List<ChatMessage> messages) {
        // 1. 调用 LLM 输出工具调用 JSON
        String llmOutput = callLLM(messages);
        return parseDecision(llmOutput, messages);
    }

    private String callLLM(List<ChatMessage> messages) {
        try {
            Response<AiMessage> response = chatLanguageModel.generate(messages);
            return response.content().text();
        } catch (OpenAiHttpException e) {
            log.error("Chat 大模型接口 HTTP 错误: code={}, message={}", e.code(), e.getMessage());
            throw e;
        }
    }

    private Decision parseDecision(String llmOutput, List<ChatMessage> messages) {
        // Schema 校验
        SchemaValidator.ValidationResult result = schemaValidator.validate(llmOutput);
        if (result.valid()) {
            JSONObject json = parameterFiller.fillDefaults(result.json());
            String toolName = json.getStr(ToolCallSchema.FIELD_TOOL_NAME);
            boolean needContinue = json.getBool(ToolCallSchema.FIELD_NEED_CONTINUE, false);
            return new Decision(toolName, json, needContinue, true);
        }

        // 重试
        for (int i = 0; i < MAX_SCHEMA_RETRIES; i++) {
            log.warn("Schema 校验失败，第 {} 次重试: {}", i + 1, result.errorMessage());

            String retryPrompt = String.format(
                    "你上一次的输出格式不正确，错误原因：%s。请严格按照 JSON 格式重新输出，只输出 JSON 对象。",
                    result.errorMessage()
            );
            messages.add(new UserMessage(retryPrompt));

            String retryOutput = callLLM(messages);
            result = schemaValidator.validate(retryOutput);
            if (result.valid()) {
                JSONObject json = parameterFiller.fillDefaults(result.json());
                String toolName = json.getStr(ToolCallSchema.FIELD_TOOL_NAME);
                boolean needContinue = json.getBool(ToolCallSchema.FIELD_NEED_CONTINUE, false);
                return new Decision(toolName, json, needContinue, true);
            }
            retryHandler.backoff(i);
        }

        log.warn("Schema 校验重试耗尽");
        return Decision.invalid();
    }
}