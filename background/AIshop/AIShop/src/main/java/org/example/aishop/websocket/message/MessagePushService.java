package org.example.aishop.websocket.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.websocket.session.SessionPoolManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统一消息推送服务
 * <p>
 * 封装 "在线 → 直接推送 WebSocket；离线 → 存入 Redis 离线队列" 的逻辑，
 * 所有 Handler 通过此服务发送消息，无需各自处理在线/离线分支。
 */
@Component
public class MessagePushService {

    @Autowired
    private SessionPoolManager sessionPool;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 推送消息给买家：在线则直接发送，离线则存入 Redis
     */
    public void pushToBuyer(Long buyerUid, Map<String, Object> message) {
        WebSocketSession session = sessionPool.getBuyerSession(buyerUid);
        if (session != null && session.isOpen()) {
            sendJson(session, message);
        } else {
            String offlineKey = RedisConstant.offlineMsgKey("user:" + buyerUid);
            pushToOfflineQueue(offlineKey, message);
        }
    }

    /**
     * 推送消息给商家（通过商家表ID）：在线则直接发送，离线则存入 Redis
     */
    public void pushToMerchant(Long merchantTableId, Map<String, Object> message) {
        WebSocketSession session = sessionPool.getMerchantSession(merchantTableId);
        if (session != null && session.isOpen()) {
            sendJson(session, message);
        } else {
            String offlineKey = RedisConstant.offlineMsgKey("merchant:" + merchantTableId);
            pushToOfflineQueue(offlineKey, message);
        }
    }

    /**
     * 推送消息给管理员：在线则直接发送，离线则存入 Redis
     */
    public void pushToAdmin(Long adminId, Map<String, Object> message) {
        WebSocketSession session = sessionPool.getAdminSession(adminId);
        if (session != null && session.isOpen()) {
            sendJson(session, message);
        } else {
            String offlineKey = RedisConstant.offlinePlatformMsgKey("admin:" + adminId);
            pushToOfflineQueue(offlineKey, message);
        }
    }

    /**
     * 补发离线消息（商家用）
     */
    public void deliverOfflineMessages(String offlineKey, WebSocketSession session) throws IOException {
        List<String> msgs = stringRedisTemplate.opsForList().range(offlineKey, 0, -1);
        if (msgs != null && !msgs.isEmpty()) {
            for (String msgJson : msgs) {
                session.sendMessage(new TextMessage(msgJson));
            }
            stringRedisTemplate.delete(offlineKey);
        }
    }

    /**
     * 发送 JSON 消息到指定 session
     */
    public void sendJson(WebSocketSession session, Map<String, Object> msg) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            } catch (IOException e) {
                System.err.println("[MessagePush] 发送消息失败: " + e.getMessage());
            }
        }
    }

    // ==================== 内部方法 ====================

    private void pushToOfflineQueue(String key, Map<String, Object> message) {
        try {
            stringRedisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message));
            stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            System.err.println("[MessagePush] 离线消息存储失败: " + e.getMessage());
        }
    }
}