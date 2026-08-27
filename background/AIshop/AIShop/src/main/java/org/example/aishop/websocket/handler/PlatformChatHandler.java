package org.example.aishop.websocket.handler;

import org.example.aishop.entity.chat.ChatPlatformMsg;
import org.example.aishop.entity.chat.ChatPlatformSession;
import org.example.aishop.service.chat.AIChatService;
import org.example.aishop.websocket.base.BaseWebSocketHandler;
import org.example.aishop.websocket.message.ChatMessage;
import org.example.aishop.websocket.message.MessagePushService;
import org.example.aishop.websocket.session.OnlineStatusManager;
import org.example.aishop.websocket.session.SessionPoolManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;

/**
 * 平台客服 ↔ 买家 聊天 Handler
 * <p>
 * 继承 BaseWebSocketHandler，只实现三个抽象方法。
 * Session 管理、在线状态、消息推送等职责委托给对应的 Service。
 */
@Component
public class PlatformChatHandler extends BaseWebSocketHandler {

    @Autowired
    private SessionPoolManager sessionPool;

    @Autowired
    private OnlineStatusManager onlineStatus;

    @Autowired
    private MessagePushService pushService;

    @Autowired
    private AIChatService chatService;

    // 买家 uid → sessionId 映射
    private final Map<Long, Long> buyerToSessionId = new HashMap<>();

    // ==================== 连接建立 ====================

    @Override
    protected void onConnected(WebSocketSession session, Long uid, String type) throws Exception {
        if (ChatMessage.ROLE_ADMIN.equals(type)) {
            handleAdminConnected(session, uid);
        } else {
            handleBuyerConnected(session, uid);
        }
    }

    private void handleAdminConnected(WebSocketSession session, Long uid) throws Exception {
        // 注册到连接池
        sessionPool.registerAdmin(uid, session);

        // 标记在线
        onlineStatus.markAdminOnline(uid);

        // 补发所有离线消息
        pushService.deliverOfflineMessages(
                "AISHOP:OFFLINE:PLATFORM:admin:" + uid, session);

        // 通知所有在线买家：管理员上线
        synchronized (buyerToSessionId) {
            for (Map.Entry<Long, Long> entry : buyerToSessionId.entrySet()) {
                WebSocketSession buyerSession = sessionPool.getBuyerSession(entry.getKey());
                if (buyerSession != null && buyerSession.isOpen()) {
                    pushService.sendJson(buyerSession,
                            ChatMessage.status(ChatMessage.TYPE_ADMIN_ONLINE, entry.getValue(), "平台客服已上线，可以继续咨询"));
                }
            }
        }

        // 发送连接确认
        pushService.sendJson(session,
                ChatMessage.status(ChatMessage.TYPE_CONNECTED, null, "已连接客服工作台"));
    }

    private void handleBuyerConnected(WebSocketSession session, Long uid) throws Exception {
        // 注册到连接池（不需要 targetMerchantId）
        sessionPool.registerBuyer(uid, session, null);

        // 创建或查找会话
        ChatPlatformSession platformSession = chatService.getOrCreatePlatformSession(uid);
        synchronized (buyerToSessionId) {
            buyerToSessionId.put(uid, platformSession.getId());
        }

        // 查找在线管理员
        Set<String> onlineAdmins = onlineStatus.getOnlineAdmins();
        boolean adminOnline = onlineAdmins != null && !onlineAdmins.isEmpty();

        if (adminOnline && platformSession.getAdminId() == null) {
            // 分配一位管理员
            Long adminId = Long.parseLong(onlineAdmins.iterator().next());
            chatService.assignAdmin(platformSession.getId(), adminId);
            platformSession.setAdminId(adminId);
        }

        // 发送连接确认
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", platformSession.getId());
        data.put("online", adminOnline);
        data.put("adminId", platformSession.getAdminId());
        if (!adminOnline) {
            data.put("message", "平台客服离线，消息已留存，工作人员上线后回复");
        }
        pushService.sendJson(session, ChatMessage.connected(platformSession.getId(), data));

        // 发送历史消息
        sendHistory(session, platformSession.getId());

        // 通知所有在线管理员：有新用户咨询
        Map<String, Object> newSessionMsg = new HashMap<>();
        newSessionMsg.put("type", ChatMessage.TYPE_NEW_SESSION);
        newSessionMsg.put("sessionId", platformSession.getId());
        newSessionMsg.put("userId", uid);
        newSessionMsg.put("createTime", platformSession.getCreateTime().getTime());
        for (Map.Entry<Long, WebSocketSession> adminEntry : sessionPool.getAllAdminSessions().entrySet()) {
            WebSocketSession adminSession = adminEntry.getValue();
            if (adminSession != null && adminSession.isOpen()) {
                pushService.sendJson(adminSession, newSessionMsg);
            }
        }
    }

    // ==================== 消息处理 ====================

    @Override
    protected void onChatMessage(Long uid, String type, Map<String, Object> msgMap) throws IOException {
        String content = (String) msgMap.get("content");
        if (content == null || content.isEmpty()) return;

        if (ChatMessage.ROLE_ADMIN.equals(type)) {
            handleAdminMessage(uid, msgMap, content);
        } else {
            handleBuyerMessage(uid, msgMap, content);
        }
    }

    private void handleAdminMessage(Long uid, Map<String, Object> msgMap, String content) throws IOException {
        Long sessionId = toLong(msgMap.get("sessionId"));
        Long toUid = toLong(msgMap.get("toUid"));
        if (sessionId == null || toUid == null) return;

        // 持久化
        ChatPlatformMsg msg = savePlatformMsg(sessionId, ChatMessage.SEND_TYPE_ADMIN, uid, toUid, msgMap, content);

        // 推送消息给买家
        Map<String, Object> pushData = ChatMessage.pushData(
                sessionId, msg.getId(), ChatMessage.SEND_TYPE_ADMIN, uid, toUid,
                msg.getMsgType(), content, null, msg.getCreateTime());
        pushService.pushToBuyer(toUid, pushData);
    }

    private void handleBuyerMessage(Long uid, Map<String, Object> msgMap, String content) throws IOException {
        Long sessionId;
        synchronized (buyerToSessionId) {
            sessionId = buyerToSessionId.get(uid);
        }
        if (sessionId == null) return;

        ChatPlatformSession platformSession = chatService.getPlatformSession(sessionId);
        if (platformSession == null) return;

        Long adminId = platformSession.getAdminId();
        if (adminId == null) {
            // 尝试分配管理员
            Set<String> onlineAdmins = onlineStatus.getOnlineAdmins();
            if (onlineAdmins != null && !onlineAdmins.isEmpty()) {
                adminId = Long.parseLong(onlineAdmins.iterator().next());
                chatService.assignAdmin(sessionId, adminId);
            }
        }

        // 持久化
        ChatPlatformMsg msg = savePlatformMsg(sessionId, ChatMessage.SEND_TYPE_BUYER, uid,
                adminId != null ? adminId : uid, msgMap, content);

        // 推送消息给管理员
        Map<String, Object> pushData = ChatMessage.pushData(
                sessionId, msg.getId(), ChatMessage.SEND_TYPE_BUYER, uid,
                adminId != null ? adminId : 0L,
                msg.getMsgType(), content, null, msg.getCreateTime());

        if (adminId != null) {
            pushService.pushToAdmin(adminId, pushData);
        }
    }

    // ==================== 会话操作 ====================

    @Override
    protected void onCloseSession(Map<String, Object> msgMap) {
        Long sessionId = toLong(msgMap.get("sessionId"));
        if (sessionId != null) {
            chatService.closePlatformSession(sessionId);
        }
    }

    @Override
    protected void onMarkRead(Map<String, Object> msgMap) {
        Long sessionId = toLong(msgMap.get("sessionId"));
        Long uid = toLong(msgMap.get("uid"));
        if (sessionId != null && uid != null) {
            chatService.markPlatformMsgRead(sessionId, uid);
        }
    }

    // ==================== 连接断开 ====================

    @Override
    protected void onDisconnected(WebSocketSession session, Long uid, String type) {
        if (ChatMessage.ROLE_ADMIN.equals(type)) {
            handleAdminDisconnected(uid);
        } else {
            handleBuyerDisconnected(uid);
        }
    }

    private void handleAdminDisconnected(Long uid) {
        sessionPool.removeAdmin(uid);
        onlineStatus.markAdminOffline(uid);

        // 通知所有在线买家：管理员离线
        synchronized (buyerToSessionId) {
            for (Map.Entry<Long, Long> entry : buyerToSessionId.entrySet()) {
                WebSocketSession buyerSession = sessionPool.getBuyerSession(entry.getKey());
                if (buyerSession != null && buyerSession.isOpen()) {
                    pushService.sendJson(buyerSession,
                            ChatMessage.status(ChatMessage.TYPE_ADMIN_OFFLINE, entry.getValue(), "平台客服已下线，消息已留存"));
                }
            }
        }
    }

    private void handleBuyerDisconnected(Long uid) {
        sessionPool.removeBuyer(uid);
        synchronized (buyerToSessionId) {
            buyerToSessionId.remove(uid);
        }
    }

    // ==================== 内部辅助 ====================

    private ChatPlatformMsg savePlatformMsg(Long sessionId, int sendType, Long sendId, Long receiveId,
                                            Map<String, Object> msgMap, String content) {
        ChatPlatformMsg msg = new ChatPlatformMsg();
        msg.setSessionId(sessionId);
        msg.setSendType(sendType);
        msg.setSendId(sendId);
        msg.setReceiveId(receiveId);
        msg.setMsgType(toInt(msgMap.get("msgType"), 1));
        msg.setContent(content);
        return chatService.savePlatformMsg(msg);
    }

    private void sendHistory(WebSocketSession session, Long sessionId) throws IOException {
        List<ChatPlatformMsg> history = chatService.listPlatformMsgs(sessionId);
        if (history != null && !history.isEmpty()) {
            List<Map<String, Object>> historyList = new ArrayList<>();
            for (ChatPlatformMsg m : history) {
                historyList.add(ChatMessage.historyItem(
                        m.getId(), m.getSessionId(), m.getSendType(), m.getSendId(), m.getReceiveId(),
                        m.getMsgType(), m.getContent(), null, m.getCreateTime()));
            }
            pushService.sendJson(session, ChatMessage.history(historyList));
        }
    }
}