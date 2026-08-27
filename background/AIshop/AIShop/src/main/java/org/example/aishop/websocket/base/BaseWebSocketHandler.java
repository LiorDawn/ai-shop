package org.example.aishop.websocket.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

/**
 * WebSocket Handler 抽象基类
 * <p>
 * 职责：
 * 1. 提供公共辅助方法（buildMsg / sendJson / toLong / toInt）
 * 2. 定义消息路由模板（handleTextMessage → switch msgType → 分发到子类方法）
 * 3. 管理连接生命周期（afterConnectionEstablished / afterConnectionClosed）
 * <p>
 * 子类只需实现三个抽象方法即可完成业务逻辑：
 * - onConnected()    连接建立
 * - onChatMessage()  收到聊天消息
 * - onDisconnected() 连接断开
 */
public abstract class BaseWebSocketHandler extends TextWebSocketHandler {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 模板方法：子类实现 ====================

    /** 连接建立后的业务逻辑 */
    protected abstract void onConnected(WebSocketSession session, Long uid, String type) throws Exception;

    /** 收到聊天消息 */
    protected abstract void onChatMessage(Long uid, String type, Map<String, Object> msgMap) throws IOException;

    /** 连接关闭后的业务逻辑 */
    protected abstract void onDisconnected(WebSocketSession session, Long uid, String type);

    // ==================== 生命周期模板 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long uid = (Long) session.getAttributes().get("uid");
        String type = (String) session.getAttributes().get("type");

        if (uid == null) {
            session.close();
            return;
        }

        onConnected(session, uid, type);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long uid = (Long) session.getAttributes().get("uid");
        String type = (String) session.getAttributes().get("type");

        if (uid == null) return;

        onDisconnected(session, uid, type);
    }

    // ==================== 消息路由模板 ====================

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long uid = (Long) session.getAttributes().get("uid");
        String type = (String) session.getAttributes().get("type");

        if (uid == null) return;

        Map<String, Object> msgMap;
        try {
            msgMap = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception e) {
            return;
        }

        String msgType = (String) msgMap.get("type");
        if (msgType == null) return;

        switch (msgType) {
            case "message":
                onChatMessage(uid, type, msgMap);
                break;
            case "close_session":
                onCloseSession(msgMap);
                break;
            case "mark_read":
                onMarkRead(msgMap);
                break;
        }
    }

    /** 关闭会话（子类可按需重写） */
    protected void onCloseSession(Map<String, Object> msgMap) {
        // 默认空实现
    }

    /** 标记已读（子类可按需重写） */
    protected void onMarkRead(Map<String, Object> msgMap) {
        // 默认空实现
    }

    // ==================== 公共辅助方法 ====================

    protected Map<String, Object> buildMsg(String type, Long sessionId, String message, Map<String, Object> data) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("type", type);
        if (sessionId != null) result.put("sessionId", sessionId);
        if (message != null) result.put("message", message);
        if (data != null) result.putAll(data);
        return result;
    }

    protected void sendJson(WebSocketSession session, Map<String, Object> msg) throws IOException {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    protected Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Integer toInt(Object obj, Integer defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}