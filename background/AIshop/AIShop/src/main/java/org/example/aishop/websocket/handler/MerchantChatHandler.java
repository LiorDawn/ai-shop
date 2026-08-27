package org.example.aishop.websocket.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.chat.ChatMerchantMsg;
import org.example.aishop.entity.chat.ChatMerchantSession;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.mapper.merchant.MerchantMapper;
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
 * 商家 ↔ 买家 聊天 Handler
 * <p>
 * 继承 BaseWebSocketHandler，只实现三个抽象方法，业务逻辑清晰。
 * Session 管理、在线状态、消息推送等职责委托给对应的 Service。
 */
@Component
public class MerchantChatHandler extends BaseWebSocketHandler {

    @Autowired
    private SessionPoolManager sessionPool;

    @Autowired
    private OnlineStatusManager onlineStatus;

    @Autowired
    private MessagePushService pushService;

    @Autowired
    private AIChatService chatService;

    @Autowired
    private MerchantMapper merchantMapper;

    // ==================== 连接建立 ====================

    @Override
    protected void onConnected(WebSocketSession session, Long uid, String type) throws Exception {
        if (ChatMessage.ROLE_MERCHANT.equals(type)) {
            handleMerchantConnected(session, uid);
        } else {
            handleBuyerConnected(session, uid);
        }
    }

    private void handleMerchantConnected(WebSocketSession session, Long uid) throws Exception {
        // 注册到连接池
        sessionPool.registerMerchant(uid, session);

        // 标记在线状态
        onlineStatus.markMerchantOnline(uid);

        // 查询商家记录，绑定 merchantId → userId 映射
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, uid));
        if (merchant != null) {
            onlineStatus.markMerchantIdOnline(merchant.getId());
            sessionPool.bindMerchantIdToUserId(merchant.getId(), uid);
            System.out.println("[MerchantChat] 商家连接: 用户表ID=" + uid + ", 商家表ID=" + merchant.getId());
        } else {
            System.out.println("[MerchantChat] 商家连接但未找到商家记录: uid=" + uid);
        }

        // 通知所有正在和该商家聊天的买家：商家上线
        for (Map.Entry<Long, Long> entry : sessionPool.getBuyerMerchantMappings()) {
            Long targetMerchantId = entry.getValue();
            Long targetUserId = sessionPool.getUserIdByMerchantId(targetMerchantId);
            if (uid.equals(targetUserId != null ? targetUserId : targetMerchantId)) {
                WebSocketSession buyerSession = sessionPool.getBuyerSession(entry.getKey());
                if (buyerSession != null && buyerSession.isOpen()) {
                    pushService.sendJson(buyerSession,
                            ChatMessage.status(ChatMessage.TYPE_MERCHANT_ONLINE, null, null));
                }
            }
        }

        // 补发离线消息
        String offlineKey = merchant != null
                ? RedisConstant.offlineMsgKey("merchant:" + merchant.getId())
                : RedisConstant.offlineMsgKey("merchant:" + uid);
        pushService.deliverOfflineMessages(offlineKey, session);

        // 发送连接确认
        pushService.sendJson(session,
                ChatMessage.status(ChatMessage.TYPE_CONNECTED, null, "已连接客服通道"));
    }

    private void handleBuyerConnected(WebSocketSession session, Long uid) throws Exception {
        Long targetMerchantId = (Long) session.getAttributes().get("targetMerchantId");

        // 注册到连接池
        sessionPool.registerBuyer(uid, session, targetMerchantId);

        // 标记在线
        onlineStatus.markBuyerOnline(uid);

        System.out.println("[MerchantChat] 买家连接: uid=" + uid + ", targetMerchantId=" + targetMerchantId);

        if (targetMerchantId != null) {
            // 创建/查找会话
            ChatMerchantSession chatSession = chatService.getOrCreateMerchantSession(uid, targetMerchantId, null);

            // 检查商家在线状态
            boolean merchantOnline = onlineStatus.isMerchantOnline(targetMerchantId);
            if (!merchantOnline) {
                merchantOnline = sessionPool.isMerchantOnlineByMerchantId(targetMerchantId);
            }

            // 发送连接确认
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", chatSession.getId());
            data.put("merchantOnline", merchantOnline);
            pushService.sendJson(session, ChatMessage.connected(chatSession.getId(), data));

            // 发送历史消息
            sendHistory(session, chatSession.getId());
        } else {
            pushService.sendJson(session,
                    ChatMessage.status(ChatMessage.TYPE_CONNECTED, null, "已连接"));
        }
    }

    // ==================== 消息处理 ====================

    @Override
    protected void onChatMessage(Long uid, String type, Map<String, Object> msgMap) throws IOException {
        String content = (String) msgMap.get("content");
        if (content == null || content.isEmpty()) return;

        if (ChatMessage.ROLE_MERCHANT.equals(type)) {
            handleMerchantMessage(uid, msgMap, content);
        } else {
            handleBuyerMessage(uid, msgMap, content);
        }
    }

    private void handleMerchantMessage(Long uid, Map<String, Object> msgMap, String content) throws IOException {
        Long sessionId = toLong(msgMap.get("sessionId"));
        Long toUid = toLong(msgMap.get("toUid"));
        if (sessionId == null || toUid == null) return;

        // 持久化
        ChatMerchantMsg msg = saveMerchantMsg(sessionId, ChatMessage.SEND_TYPE_MERCHANT, uid, toUid, msgMap, content);

        // 推送消息给买家
        Map<String, Object> pushData = ChatMessage.pushData(
                sessionId, msg.getId(), ChatMessage.SEND_TYPE_MERCHANT, uid, toUid,
                msg.getMsgType(), content, msg.getExtraData(), msg.getCreateTime());
        pushService.pushToBuyer(toUid, pushData);
    }

    private void handleBuyerMessage(Long uid, Map<String, Object> msgMap, String content) throws IOException {
        Long sessionId = toLong(msgMap.get("sessionId"));
        Long targetMerchantId = sessionPool.getBuyerTargetMerchantId(uid);
        System.out.println("[MerchantChat] 买家发消息: uid=" + uid + ", sessionId=" + sessionId + ", targetMerchantId=" + targetMerchantId);
        if (sessionId == null || targetMerchantId == null) return;

        // 持久化
        ChatMerchantMsg msg = saveMerchantMsg(sessionId, ChatMessage.SEND_TYPE_BUYER, uid, targetMerchantId, msgMap, content);

        // 推送消息给商家
        Map<String, Object> pushData = ChatMessage.pushData(
                sessionId, msg.getId(), ChatMessage.SEND_TYPE_BUYER, uid, targetMerchantId,
                msg.getMsgType(), content, msg.getExtraData(), msg.getCreateTime());
        pushService.pushToMerchant(targetMerchantId, pushData);
    }

    // ==================== 会话操作 ====================

    @Override
    protected void onCloseSession(Map<String, Object> msgMap) {
        Long sessionId = toLong(msgMap.get("sessionId"));
        if (sessionId != null) {
            chatService.closeMerchantSession(sessionId);
        }
    }

    @Override
    protected void onMarkRead(Map<String, Object> msgMap) {
        Long sessionId = toLong(msgMap.get("sessionId"));
        Long uid = toLong(msgMap.get("uid"));
        if (sessionId != null && uid != null) {
            chatService.markMerchantMsgRead(sessionId, uid);
        }
    }

    // ==================== 连接断开 ====================

    @Override
    protected void onDisconnected(WebSocketSession session, Long uid, String type) {
        if (ChatMessage.ROLE_MERCHANT.equals(type)) {
            handleMerchantDisconnected(uid, session);
        } else {
            handleBuyerDisconnected(uid);
        }
    }

    private void handleMerchantDisconnected(Long uid, WebSocketSession session) {
        boolean allDisconnected = sessionPool.removeMerchant(uid, session);

        if (allDisconnected) {
            onlineStatus.markMerchantOffline(uid);

            // 查询商家记录，同步清理 merchantId 映射
            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, uid));
            if (merchant != null) {
                onlineStatus.markMerchantIdOffline(merchant.getId());
                sessionPool.unbindMerchantIdToUserId(merchant.getId());
            }

            // 通知所有连接的买家：商家离线
            for (Map.Entry<Long, Long> entry : sessionPool.getBuyerMerchantMappings()) {
                Long targetUserId = sessionPool.getUserIdByMerchantId(entry.getValue());
                if (uid.equals(targetUserId != null ? targetUserId : entry.getValue())) {
                    WebSocketSession buyerSession = sessionPool.getBuyerSession(entry.getKey());
                    if (buyerSession != null && buyerSession.isOpen()) {
                        pushService.sendJson(buyerSession,
                                ChatMessage.status(ChatMessage.TYPE_MERCHANT_OFFLINE, null, "商家已离线，消息将留存待回复"));
                    }
                }
            }
        }
    }

    private void handleBuyerDisconnected(Long uid) {
        sessionPool.removeBuyer(uid);
        onlineStatus.markBuyerOffline(uid);
    }

    // ==================== 内部辅助 ====================

    private ChatMerchantMsg saveMerchantMsg(Long sessionId, int sendType, Long sendId, Long receiveId,
                                            Map<String, Object> msgMap, String content) {
        ChatMerchantMsg msg = new ChatMerchantMsg();
        msg.setSessionId(sessionId);
        msg.setSendType(sendType);
        msg.setSendId(sendId);
        msg.setReceiveId(receiveId);
        msg.setMsgType(toInt(msgMap.get("msgType"), 1));
        msg.setContent(content);
        return chatService.saveMerchantMsg(msg);
    }

    private void sendHistory(WebSocketSession session, Long sessionId) throws IOException {
        List<ChatMerchantMsg> history = chatService.listMerchantMsgs(sessionId);
        if (history != null && !history.isEmpty()) {
            List<Map<String, Object>> historyList = new ArrayList<>();
            for (ChatMerchantMsg m : history) {
                historyList.add(ChatMessage.historyItem(
                        m.getId(), m.getSessionId(), m.getSendType(), m.getSendId(), m.getReceiveId(),
                        m.getMsgType(), m.getContent(), m.getExtraData(), m.getCreateTime()));
            }
            pushService.sendJson(session, ChatMessage.history(historyList));
        }
    }
}