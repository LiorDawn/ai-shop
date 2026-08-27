package org.example.aishop.ai.storage.redis;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.aishop.common.constant.RedisConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 会话上下文服务
 *
 * 职责：管理对话热数据的短期存储。
 * - 最近 10 轮对话上下文缓存（TTL 1 小时）
 * - 会话列表缓存（TTL 1 小时）
 * - 优先 Redis 读取，未命中时回源 MySQL
 *
 * Redis 仅存储短期热数据，长期持久化由 MySQL 负责。
 */
@Service
public class RedisConversationService {

    private static final Logger log = LoggerFactory.getLogger(RedisConversationService.class);

    /** 上下文最大保留条数 */
    private static final int MAX_CONTEXT_MESSAGES = 10;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 加载对话上下文
     * 优先从 Redis 读取，未命中返回空列表（由编排层从 MySQL 回填）
     *
     * @return LangChain4j ChatMessage 列表
     */
    public List<ChatMessage> loadContext(Long sessionId) {
        try {
            String key = RedisConstant.aiContextKey(sessionId);
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isEmpty()) {
                JSONArray arr = JSONUtil.parseArray(cached);
                trimToMaxSize(arr);
                return convertToMessages(arr);
            }
        } catch (Exception e) {
            log.warn("Redis 读取会话上下文失败: sessionId={}", sessionId, e);
        }
        return Collections.emptyList();
    }

    /**
     * 更新对话上下文（追加最新一轮对话，裁剪到 10 条）
     */
    public void updateContext(Long sessionId, String userMessage, String reply, String imgUrl) {
        try {
            String key = RedisConstant.aiContextKey(sessionId);
            JSONArray messages = loadContextRaw(sessionId);

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", userMessage);
            if (imgUrl != null && !imgUrl.isEmpty()) {
                userMsg.set("imgUrl", imgUrl);
            }
            messages.add(userMsg);

            JSONObject aiMsg = new JSONObject();
            aiMsg.set("role", "assistant");
            aiMsg.set("content", reply);
            messages.add(aiMsg);

            trimToMaxSize(messages);

            stringRedisTemplate.opsForValue().set(
                    key, messages.toString(),
                    RedisConstant.AI_CONTEXT_TTL_SECONDS, TimeUnit.SECONDS
            );

            log.info("Redis 上下文更新成功: sessionId={}, 当前消息数={}", sessionId, messages.size());

        } catch (Exception e) {
            log.warn("Redis 更新会话上下文失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 删除会话上下文缓存
     */
    public void deleteContext(Long sessionId) {
        try {
            stringRedisTemplate.delete(RedisConstant.aiContextKey(sessionId));
        } catch (Exception ignored) {}
    }

    /**
     * 删除用户会话列表缓存
     */
    public void deleteSessionListCache(Long userId) {
        try {
            stringRedisTemplate.delete(RedisConstant.aiSessionListKey(userId));
        } catch (Exception ignored) {}
    }

    // ==================== 私有方法 ====================

    private JSONArray loadContextRaw(Long sessionId) {
        try {
            String key = RedisConstant.aiContextKey(sessionId);
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isEmpty()) {
                return JSONUtil.parseArray(cached);
            }
        } catch (Exception ignored) {}
        return new JSONArray();
    }

    private void trimToMaxSize(JSONArray messages) {
        while (messages.size() > MAX_CONTEXT_MESSAGES) {
            messages.remove(0);
        }
    }

    /**
     * 将 JSONArray 转换为 LangChain4j ChatMessage 列表
     */
    private List<ChatMessage> convertToMessages(JSONArray jsonMessages) {
        List<ChatMessage> result = new ArrayList<>();
        for (int i = 0; i < jsonMessages.size(); i++) {
            JSONObject msg = jsonMessages.getJSONObject(i);
            String role = msg.getStr("role");
            String content = msg.getStr("content", "");

            if ("user".equals(role)) {
                result.add(new UserMessage(content));
            } else if ("assistant".equals(role)) {
                result.add(new AiMessage(content));
            }
        }
        return result;
    }
}