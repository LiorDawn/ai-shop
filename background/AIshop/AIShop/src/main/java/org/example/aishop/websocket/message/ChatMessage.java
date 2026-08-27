package org.example.aishop.websocket.message;

import java.util.*;

/**
 * 聊天消息协议定义
 * <p>
 * 统一管理所有 WebSocket 消息类型常量和构建方法，
 * 避免散落在各 Handler 中的魔法字符串。
 */
public final class ChatMessage {

    private ChatMessage() {}

    // ==================== 消息类型 ====================

    public static final String TYPE_CONNECTED        = "connected";
    public static final String TYPE_MESSAGE          = "message";
    public static final String TYPE_HISTORY          = "history";
    public static final String TYPE_CLOSE_SESSION    = "close_session";
    public static final String TYPE_MARK_READ        = "mark_read";
    public static final String TYPE_MERCHANT_ONLINE  = "merchant_online";
    public static final String TYPE_MERCHANT_OFFLINE = "merchant_offline";
    public static final String TYPE_ADMIN_ONLINE     = "admin_online";
    public static final String TYPE_ADMIN_OFFLINE    = "admin_offline";
    public static final String TYPE_NEW_SESSION      = "new_session";

    // ==================== 发送类型 ====================

    public static final int SEND_TYPE_BUYER    = 1;
    public static final int SEND_TYPE_MERCHANT = 2;
    public static final int SEND_TYPE_ADMIN    = 2;
    public static final int SEND_TYPE_SYSTEM   = 0;

    // ==================== 角色常量 ====================

    public static final String ROLE_BUYER    = "user";
    public static final String ROLE_MERCHANT = "merchant";
    public static final String ROLE_ADMIN    = "admin";

    // ==================== 构建方法 ====================

    public static Map<String, Object> connected(Long sessionId, Map<String, Object> data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", TYPE_CONNECTED);
        if (sessionId != null) msg.put("sessionId", sessionId);
        if (data != null) msg.putAll(data);
        return msg;
    }

    public static Map<String, Object> status(String type, Long sessionId, String message) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        if (sessionId != null) msg.put("sessionId", sessionId);
        if (message != null) msg.put("message", message);
        return msg;
    }

    public static Map<String, Object> history(List<Map<String, Object>> messages) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", TYPE_HISTORY);
        msg.put("messages", messages != null ? messages : Collections.emptyList());
        return msg;
    }

    /**
     * 构建推送消息数据
     */
    public static Map<String, Object> pushData(Long sessionId, Long messageId,
                                                int sendType, Long sendId, Long receiveId,
                                                int msgType, String content,
                                                String extraData, Date createTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", TYPE_MESSAGE);
        data.put("sessionId", sessionId);
        data.put("messageId", messageId);
        data.put("sendType", sendType);
        data.put("sendId", sendId);
        data.put("receiveId", receiveId);
        data.put("msgType", msgType);
        data.put("content", content);
        data.put("extraData", extraData);
        data.put("createTime", createTime != null ? createTime.getTime() : null);
        return data;
    }

    /**
     * 构建历史消息列表项
     */
    public static Map<String, Object> historyItem(Long id, Long sessionId,
                                                   int sendType, Long sendId, Long receiveId,
                                                   int msgType, String content,
                                                   String extraData, Date createTime) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("sessionId", sessionId);
        item.put("sendType", sendType);
        item.put("sendId", sendId);
        item.put("receiveId", receiveId);
        item.put("msgType", msgType);
        item.put("content", content);
        item.put("extraData", extraData);
        item.put("createTime", createTime != null ? createTime.getTime() : null);
        return item;
    }
}