package org.example.aishop.ai.storage.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.aishop.entity.ai.AIChatMessage;
import org.example.aishop.entity.ai.AISession;
import org.example.aishop.mapper.ai.AIChatMapper;
import org.example.aishop.mapper.ai.AISessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * MySQL 持久化存储服务
 *
 * 职责：负责所有需要长期保存的数据。
 * - 会话信息（ai_session 表）
 * - 聊天消息（ai_message 表）
 * - 工具调用操作日志
 *
 * Redis 中的数据过期后，可以从 MySQL 全量恢复。
 */
@Service
public class MysqlPersistService {

    private static final Logger log = LoggerFactory.getLogger(MysqlPersistService.class);

    @Autowired
    private AISessionMapper aiSessionMapper;

    @Autowired
    private AIChatMapper aiChatMapper;

    // ==================== 会话管理 ====================

    @Transactional
    public void insertSession(AISession session) {
        aiSessionMapper.insert(session);
        log.info("MySQL 创建会话: sessionId={}", session.getId());
    }

    public AISession getSession(Long sessionId) {
        return aiSessionMapper.selectById(sessionId);
    }

    public List<AISession> getUserSessions(Long userId) {
        return aiSessionMapper.selectList(
                new LambdaQueryWrapper<AISession>()
                        .eq(AISession::getUserId, userId)
                        .orderByDesc(AISession::getLastTime)
        );
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        aiSessionMapper.deleteById(sessionId);
        log.info("MySQL 删除会话: sessionId={}", sessionId);
    }

    @Transactional
    public void updateSessionLastTime(Long sessionId) {
        aiSessionMapper.update(null,
                new LambdaUpdateWrapper<AISession>()
                        .set(AISession::getLastTime, new Date())
                        .eq(AISession::getId, sessionId));
    }

    // ==================== 消息管理 ====================

    @Transactional
    public void saveMessage(Long sessionId, String role, String content, String imgUrl) {
        AIChatMessage msg = new AIChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setImgUrl(imgUrl);
        msg.setCreateTime(new Date());
        aiChatMapper.insert(msg);
    }

    /**
     * 查询会话最近 N 条消息（倒序，最新的在前）
     */
    public List<AIChatMessage> getRecentMessages(Long sessionId, int limit) {
        return aiChatMapper.selectList(
                new LambdaQueryWrapper<AIChatMessage>()
                        .eq(AIChatMessage::getSessionId, sessionId)
                        .orderByDesc(AIChatMessage::getCreateTime)
                        .last("LIMIT " + limit)
        );
    }

    public List<AIChatMessage> getSessionMessages(Long sessionId) {
        return aiChatMapper.selectList(
                new LambdaQueryWrapper<AIChatMessage>()
                        .eq(AIChatMessage::getSessionId, sessionId)
                        .orderByAsc(AIChatMessage::getCreateTime)
        );
    }
}