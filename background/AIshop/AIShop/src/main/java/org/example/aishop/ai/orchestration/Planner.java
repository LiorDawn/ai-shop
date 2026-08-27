package org.example.aishop.ai.orchestration;

import cn.hutool.json.JSONObject;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 规划层接口 — ReAct 推理、工具决策、意图分类
 *
 * 职责：
 * 1. 前置意图分类：快速判断用户是否需要工具调用
 * 2. 工具决策：调用 LLM → 输出结构化工具调用 JSON
 * 3. Schema 校验 + 重试
 */
public interface Planner {

    /**
     * 工具决策结果
     */
    record Decision(String toolName, JSONObject parameters, boolean needContinue, boolean valid) {
        public static Decision none() {
            return new Decision("none", new JSONObject(), false, true);
        }
        public static Decision invalid() {
            return new Decision(null, null, false, false);
        }
    }

    /**
     * 前置意图分类：判断用户消息是否需要工具调用
     *
     * @return true=需要工具调用，false=纯闲聊/知识问答
     */
    boolean classifyIntent(String userMessage);

    /**
     * ReAct 推理：调用 LLM 输出工具调用决策
     *
     * @param messages 当前完整消息上下文
     * @return 工具调用决策（含 Schema 校验结果）
     */
    Decision plan(List<ChatMessage> messages);
}