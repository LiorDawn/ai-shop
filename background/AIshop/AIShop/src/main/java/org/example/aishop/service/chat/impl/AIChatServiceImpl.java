package org.example.aishop.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.aishop.entity.chat.ChatMerchantMsg;
import org.example.aishop.entity.chat.ChatMerchantSession;
import org.example.aishop.entity.chat.ChatPlatformMsg;
import org.example.aishop.entity.chat.ChatPlatformSession;
import org.example.aishop.mapper.chat.ChatMerchantMsgMapper;
import org.example.aishop.mapper.chat.ChatMerchantSessionMapper;
import org.example.aishop.mapper.chat.ChatPlatformMsgMapper;
import org.example.aishop.mapper.chat.ChatPlatformSessionMapper;
import org.example.aishop.service.chat.AIChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIChatServiceImpl implements AIChatService {

    @Autowired
    private ChatMerchantSessionMapper merchantSessionMapper;
    @Autowired
    private ChatMerchantMsgMapper merchantMsgMapper;
    @Autowired
    private ChatPlatformSessionMapper platformSessionMapper;
    @Autowired
    private ChatPlatformMsgMapper platformMsgMapper;

    // ==================== 商家客服会话 ====================

    @Override
    @Transactional
    public ChatMerchantSession getOrCreateMerchantSession(Long userId, Long merchantId, Long shopId) {
        LambdaQueryWrapper<ChatMerchantSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMerchantSession::getUserId, userId)
               .eq(ChatMerchantSession::getMerchantId, merchantId)
               .orderByDesc(ChatMerchantSession::getCreateTime)
               .last("LIMIT 1");
        ChatMerchantSession session = merchantSessionMapper.selectOne(wrapper);
        if (session != null) {
            if (session.getStatus() != null && session.getStatus() == 1) {
                session.setStatus(0);
                merchantSessionMapper.updateById(session);
            }
            return session;
        }

        session = new ChatMerchantSession();
        session.setUserId(userId);
        session.setMerchantId(merchantId);
        session.setShopId(shopId);
        session.setStatus(0);
        session.setCreateTime(new Date());
        merchantSessionMapper.insert(session);
        return session;
    }

    @Override
    public List<ChatMerchantSession> listMerchantSessionsByUser(Long userId) {
        LambdaQueryWrapper<ChatMerchantSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMerchantSession::getUserId, userId)
               .orderByDesc(ChatMerchantSession::getCreateTime);
        return merchantSessionMapper.selectList(wrapper);
    }

    @Override
    public List<ChatMerchantSession> listMerchantSessionsByMerchant(Long merchantId) {
        LambdaQueryWrapper<ChatMerchantSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMerchantSession::getMerchantId, merchantId)
               .orderByDesc(ChatMerchantSession::getCreateTime);
        return merchantSessionMapper.selectList(wrapper);
    }

    @Override
    public ChatMerchantSession getMerchantSession(Long sessionId) {
        return merchantSessionMapper.selectById(sessionId);
    }

    @Override
    public void closeMerchantSession(Long sessionId) {
        ChatMerchantSession session = new ChatMerchantSession();
        session.setId(sessionId);
        session.setStatus(1);
        session.setEndTime(new Date());
        merchantSessionMapper.updateById(session);
    }

    // ==================== 商家客服消息 ====================

    @Override
    public ChatMerchantMsg saveMerchantMsg(ChatMerchantMsg msg) {
        msg.setCreateTime(new Date());
        msg.setIsRead(0);
        merchantMsgMapper.insert(msg);
        return msg;
    }

    @Override
    public List<ChatMerchantMsg> listMerchantMsgs(Long sessionId) {
        LambdaQueryWrapper<ChatMerchantMsg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMerchantMsg::getSessionId, sessionId)
               .orderByAsc(ChatMerchantMsg::getCreateTime);
        return merchantMsgMapper.selectList(wrapper);
    }

    @Override
    public void markMerchantMsgRead(Long sessionId, Long readerId) {
        LambdaUpdateWrapper<ChatMerchantMsg> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatMerchantMsg::getSessionId, sessionId)
               .ne(ChatMerchantMsg::getSendId, readerId)
               .eq(ChatMerchantMsg::getIsRead, 0)
               .set(ChatMerchantMsg::getIsRead, 1);
        int updated = merchantMsgMapper.update(null, wrapper);
        if (updated == 0) {
            // 区分：全部已读 vs 条件不匹配
            long totalUnread = merchantMsgMapper.selectCount(
                new LambdaQueryWrapper<ChatMerchantMsg>()
                    .eq(ChatMerchantMsg::getSessionId, sessionId)
                    .eq(ChatMerchantMsg::getIsRead, 0));
            if (totalUnread > 0) {
                System.out.println("[markMerchantMsgRead] 警告: 会话" + sessionId
                    + " 有" + totalUnread + "条未读消息，但 readerId=" + readerId
                    + " 未匹配任何行（可能是 sendId 与 readerId 类型不匹配）");
            }
        }
    }

    @Override
    public Map<Long, Map<String, Object>> getMerchantSessionSummaries(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 一次查询所有相关会话的消息，按时间升序
        List<ChatMerchantMsg> allMsgs = merchantMsgMapper.selectList(
                new LambdaQueryWrapper<ChatMerchantMsg>()
                        .in(ChatMerchantMsg::getSessionId, sessionIds)
                        .orderByAsc(ChatMerchantMsg::getCreateTime));

        // 按 sessionId 分组
        Map<Long, List<ChatMerchantMsg>> grouped = allMsgs.stream()
                .collect(Collectors.groupingBy(ChatMerchantMsg::getSessionId));

        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (Long sid : sessionIds) {
            List<ChatMerchantMsg> msgs = grouped.getOrDefault(sid, Collections.emptyList());
            long unread = msgs.stream()
                    .filter(m -> m.getIsRead() != null && m.getIsRead() == 0 && m.getSendType() != null && m.getSendType() != 2)
                    .count();
            String lastMsg = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).getContent();

            Map<String, Object> summary = new HashMap<>();
            summary.put("unreadCount", unread > 99 ? 99 : unread);
            summary.put("lastMessage", lastMsg);
            result.put(sid, summary);
        }
        return result;
    }

    // ==================== 平台客服会话 ====================

    @Override
    @Transactional
    public ChatPlatformSession getOrCreatePlatformSession(Long userId) {
        LambdaQueryWrapper<ChatPlatformSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatPlatformSession::getUserId, userId)
               .orderByDesc(ChatPlatformSession::getCreateTime)
               .last("LIMIT 1");
        ChatPlatformSession session = platformSessionMapper.selectOne(wrapper);
        if (session != null) {
            if (session.getStatus() != null && session.getStatus() == 1) {
                session.setStatus(0);
                platformSessionMapper.updateById(session);
            }
            return session;
        }

        session = new ChatPlatformSession();
        session.setUserId(userId);
        session.setStatus(0);
        session.setCreateTime(new Date());
        platformSessionMapper.insert(session);
        return session;
    }

    @Override
    public void assignAdmin(Long sessionId, Long adminId) {
        ChatPlatformSession session = new ChatPlatformSession();
        session.setId(sessionId);
        session.setAdminId(adminId);
        platformSessionMapper.updateById(session);
    }

    @Override
    public List<ChatPlatformSession> listPlatformSessionsByUser(Long userId) {
        LambdaQueryWrapper<ChatPlatformSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatPlatformSession::getUserId, userId)
               .orderByDesc(ChatPlatformSession::getCreateTime);
        return platformSessionMapper.selectList(wrapper);
    }

    @Override
    public List<ChatPlatformSession> listPlatformSessions() {
        LambdaQueryWrapper<ChatPlatformSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ChatPlatformSession::getCreateTime);
        return platformSessionMapper.selectList(wrapper);
    }

    @Override
    public ChatPlatformSession getPlatformSession(Long sessionId) {
        return platformSessionMapper.selectById(sessionId);
    }

    @Override
    public void closePlatformSession(Long sessionId) {
        ChatPlatformSession session = new ChatPlatformSession();
        session.setId(sessionId);
        session.setStatus(1);
        session.setEndTime(new Date());
        platformSessionMapper.updateById(session);
    }

    // ==================== 平台客服消息 ====================

    @Override
    public ChatPlatformMsg savePlatformMsg(ChatPlatformMsg msg) {
        msg.setCreateTime(new Date());
        msg.setIsRead(0);
        platformMsgMapper.insert(msg);
        return msg;
    }

    @Override
    public List<ChatPlatformMsg> listPlatformMsgs(Long sessionId) {
        LambdaQueryWrapper<ChatPlatformMsg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatPlatformMsg::getSessionId, sessionId)
               .orderByAsc(ChatPlatformMsg::getCreateTime);
        return platformMsgMapper.selectList(wrapper);
    }

    @Override
    public void markPlatformMsgRead(Long sessionId, Long readerId) {
        LambdaUpdateWrapper<ChatPlatformMsg> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatPlatformMsg::getSessionId, sessionId)
               .ne(ChatPlatformMsg::getSendId, readerId)
               .eq(ChatPlatformMsg::getIsRead, 0)
               .set(ChatPlatformMsg::getIsRead, 1);
        platformMsgMapper.update(null, wrapper);
    }
}