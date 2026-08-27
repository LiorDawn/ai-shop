package org.example.aishop.websocket.session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 统一 WebSocket 连接池管理
 * <p>
 * 管理三类角色的连接：
 * - buyer（买家）：uid → session（一对一）
 * - merchant（商家）：uid → sessions（一对多，支持同一商家多端连接）
 * - admin（平台管理员）：uid → session（一对一）
 * <p>
 * 同时维护买家 → 目标映射关系，用于消息路由和离线通知。
 */
@Component
public class SessionPoolManager {

    // ==================== 连接池 ====================

    /** 买家 uid → session */
    private final ConcurrentHashMap<Long, WebSocketSession> buyerSessions = new ConcurrentHashMap<>();

    /** 商家 uid → session 列表（支持多端） */
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<WebSocketSession>> merchantSessions = new ConcurrentHashMap<>();

    /** 管理员 uid → session */
    private final ConcurrentHashMap<Long, WebSocketSession> adminSessions = new ConcurrentHashMap<>();

    // ==================== 映射关系 ====================

    /** 买家 uid → 商家表ID（merchant.id） */
    private final ConcurrentHashMap<Long, Long> buyerToMerchantId = new ConcurrentHashMap<>();

    /** 商家表ID（merchant.id） → 用户表ID（user.id） */
    private final ConcurrentHashMap<Long, Long> merchantIdToUserId = new ConcurrentHashMap<>();

    // ==================== 注册 ====================

    public void registerBuyer(Long uid, WebSocketSession session, Long targetMerchantId) {
        buyerSessions.put(uid, session);
        if (targetMerchantId != null) {
            buyerToMerchantId.put(uid, targetMerchantId);
        }
    }

    public void registerMerchant(Long uid, WebSocketSession session) {
        merchantSessions.computeIfAbsent(uid, k -> new CopyOnWriteArrayList<>()).add(session);
    }

    public void registerAdmin(Long uid, WebSocketSession session) {
        adminSessions.put(uid, session);
    }

    /** 记录 merchantId → userId 映射，用于消息推送时查找 session */
    public void bindMerchantIdToUserId(Long merchantId, Long userId) {
        merchantIdToUserId.put(merchantId, userId);
    }

    // ==================== 查询 ====================

    public WebSocketSession getBuyerSession(Long uid) {
        return buyerSessions.get(uid);
    }

    /**
     * 获取商家可用的 WebSocket session（取第一个打开的）
     */
    public WebSocketSession getMerchantSession(Long merchantTableId) {
        // 先通过 merchantId → userId 映射找到实际 uid
        Long uid = merchantIdToUserId.getOrDefault(merchantTableId, merchantTableId);
        CopyOnWriteArrayList<WebSocketSession> sessions = merchantSessions.get(uid);
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                if (s != null && s.isOpen()) {
                    return s;
                }
            }
        }
        return null;
    }

    public WebSocketSession getAdminSession(Long uid) {
        return adminSessions.get(uid);
    }

    /** 获取买家当前连接的目标商家表ID */
    public Long getBuyerTargetMerchantId(Long buyerUid) {
        return buyerToMerchantId.get(buyerUid);
    }

    /** 获取与指定商家（用户表ID）连线的所有买家 uid */
    public Set<Map.Entry<Long, Long>> getBuyerMerchantMappings() {
        return buyerToMerchantId.entrySet();
    }

    /** 获取商家表ID对应的用户表ID */
    public Long getUserIdByMerchantId(Long merchantId) {
        return merchantIdToUserId.get(merchantId);
    }

    /** 获取所有管理员 session */
    public Map<Long, WebSocketSession> getAllAdminSessions() {
        return adminSessions;
    }

    // ==================== 移除 ====================

    public void removeBuyer(Long uid) {
        buyerSessions.remove(uid);
        buyerToMerchantId.remove(uid);
    }

    /**
     * 移除商家 session
     * @return true 表示该商家的所有连接都已断开
     */
    public boolean removeMerchant(Long uid, WebSocketSession session) {
        CopyOnWriteArrayList<WebSocketSession> list = merchantSessions.get(uid);
        if (list != null) {
            list.remove(session);
            if (list.isEmpty()) {
                merchantSessions.remove(uid);
                return true;
            }
        }
        return false;
    }

    public void removeAdmin(Long uid) {
        adminSessions.remove(uid);
    }

    /** 移除 merchantId → userId 映射 */
    public void unbindMerchantIdToUserId(Long merchantId) {
        merchantIdToUserId.remove(merchantId);
    }

    // ==================== 状态查询 ====================

    public boolean isMerchantOnlineByUserId(Long userId) {
        CopyOnWriteArrayList<WebSocketSession> sessions = merchantSessions.get(userId);
        return sessions != null && !sessions.isEmpty() && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    public boolean isMerchantOnlineByMerchantId(Long merchantId) {
        Long uid = merchantIdToUserId.get(merchantId);
        if (uid != null) {
            return isMerchantOnlineByUserId(uid);
        }
        return isMerchantOnlineByUserId(merchantId);
    }
}