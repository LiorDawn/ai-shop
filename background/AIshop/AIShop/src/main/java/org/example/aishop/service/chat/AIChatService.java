package org.example.aishop.service.chat;

import org.example.aishop.entity.chat.ChatMerchantMsg;
import org.example.aishop.entity.chat.ChatMerchantSession;
import org.example.aishop.entity.chat.ChatPlatformMsg;
import org.example.aishop.entity.chat.ChatPlatformSession;

import java.util.List;
import java.util.Map;

public interface AIChatService {

    // ==================== 商家客服会话 ====================

    /** 创建或查找商家客服会话 */
    ChatMerchantSession getOrCreateMerchantSession(Long userId, Long merchantId, Long shopId);

    /** 查询用户的所有商家客服会话 */
    List<ChatMerchantSession> listMerchantSessionsByUser(Long userId);

    /** 查询商家的所有客服会话 */
    List<ChatMerchantSession> listMerchantSessionsByMerchant(Long merchantId);

    /** 查询会话 */
    ChatMerchantSession getMerchantSession(Long sessionId);

    /** 关闭会话 */
    void closeMerchantSession(Long sessionId);

    // ==================== 商家客服消息 ====================

    /** 保存消息 */
    ChatMerchantMsg saveMerchantMsg(ChatMerchantMsg msg);

    /** 查询会话消息列表 */
    List<ChatMerchantMsg> listMerchantMsgs(Long sessionId);

    /** 标记会话消息已读 */
    void markMerchantMsgRead(Long sessionId, Long readerId);

    /**
     * 批量查询会话摘要（未读数 + 最后消息），避免 N+1 查询
     * @param sessionIds 会话ID列表
     * @return Map<sessionId, [unreadCount, lastMessage]>
     */
    Map<Long, Map<String, Object>> getMerchantSessionSummaries(List<Long> sessionIds);

    // ==================== 平台客服会话 ====================

    /** 创建平台客服会话 */
    ChatPlatformSession getOrCreatePlatformSession(Long userId);

    /** 分配管理员 */
    void assignAdmin(Long sessionId, Long adminId);

    /** 查询用户的所有平台客服会话 */
    List<ChatPlatformSession> listPlatformSessionsByUser(Long userId);

    /** 查询所有进行中的平台客服会话 */
    List<ChatPlatformSession> listPlatformSessions();

    /** 查询会话 */
    ChatPlatformSession getPlatformSession(Long sessionId);

    /** 关闭会话 */
    void closePlatformSession(Long sessionId);

    // ==================== 平台客服消息 ====================

    /** 保存平台客服消息 */
    ChatPlatformMsg savePlatformMsg(ChatPlatformMsg msg);

    /** 查询平台客服会话消息列表 */
    List<ChatPlatformMsg> listPlatformMsgs(Long sessionId);

    /** 标记会话消息已读 */
    void markPlatformMsgRead(Long sessionId, Long readerId);
}