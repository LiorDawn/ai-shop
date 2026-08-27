package org.example.aishop.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.entity.ai.AIChatMessage;
import org.example.aishop.entity.ai.AISession;
import org.example.aishop.mapper.ai.AIChatMapper;
import org.example.aishop.mapper.ai.AISessionMapper;
import org.example.aishop.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * AI 聊天会话管理服务
 *
 * 负责会话 CRUD、消息查询、图片上传等。
 */
@Service
public class AIChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(AIChatSessionService.class);

    @Autowired
    private AISessionMapper sessionMapper;

    @Autowired
    private AIChatMapper chatMapper;

    // ==================== 会话管理 ====================

    /** 获取当前用户的会话列表（按最后操作时间倒序） */
    public List<AISession> listSessions() {
        Long userId = UserHolder.getUserId();
        LambdaQueryWrapper<AISession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AISession::getUserId, userId)
                .orderByDesc(AISession::getLastTime);
        return sessionMapper.selectList(wrapper);
    }

    /** 创建新会话 */
    public AISession createSession() {
        Long userId = UserHolder.getUserId();
        AISession session = new AISession();
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setCreateTime(new Date());
        session.setLastTime(new Date());
        sessionMapper.insert(session);
        log.info("创建 AI 会话: id={}, userId={}", session.getId(), userId);
        return session;
    }

    /** 重命名会话 */
    public void renameSession(Long sessionId, String title) {
        Long userId = UserHolder.getUserId();
        LambdaUpdateWrapper<AISession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AISession::getId, sessionId)
                .eq(AISession::getUserId, userId)
                .set(AISession::getTitle, title)
                .set(AISession::getLastTime, new Date());
        sessionMapper.update(null, wrapper);
        log.info("重命名会话: id={}, title={}", sessionId, title);
    }

    /** 删除会话（同时删除关联消息） */
    public void deleteSession(Long sessionId) {
        Long userId = UserHolder.getUserId();
        // 校验会话归属
        AISession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权操作");
        }
        // 删除消息
        LambdaQueryWrapper<AIChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(AIChatMessage::getSessionId, sessionId);
        chatMapper.delete(msgWrapper);
        // 删除会话
        sessionMapper.deleteById(sessionId);
        log.info("删除 AI 会话: id={}, userId={}", sessionId, userId);
    }

    /** 获取用户最近一次会话 */
    public AISession getLatestSession() {
        Long userId = UserHolder.getUserId();
        LambdaQueryWrapper<AISession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AISession::getUserId, userId)
                .orderByDesc(AISession::getLastTime)
                .last("LIMIT 1");
        return sessionMapper.selectOne(wrapper);
    }

    /** 获取或创建会话（用于 SSE 流式对话） */
    public Long getOrCreateSession(Long sessionId) {
        if (sessionId != null) {
            AISession session = sessionMapper.selectById(sessionId);
            if (session != null) {
                // 更新最后操作时间
                session.setLastTime(new Date());
                sessionMapper.updateById(session);
                return session.getId();
            }
        }
        return createSession().getId();
    }

    /** 更新会话最后操作时间 */
    public void touchSession(Long sessionId) {
        LambdaUpdateWrapper<AISession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AISession::getId, sessionId)
                .set(AISession::getLastTime, new Date());
        sessionMapper.update(null, wrapper);
    }

    // ==================== 消息管理 ====================

    /** 获取会话的所有消息（正序） */
    public List<AIChatMessage> getMessages(Long sessionId) {
        LambdaQueryWrapper<AIChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIChatMessage::getSessionId, sessionId)
                .orderByAsc(AIChatMessage::getCreateTime);
        return chatMapper.selectList(wrapper);
    }

    /** 分页获取会话消息（倒序，最新的在前） */
    public Page<AIChatMessage> getMessagesPage(Long sessionId, int page, int size) {
        LambdaQueryWrapper<AIChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIChatMessage::getSessionId, sessionId)
                .orderByDesc(AIChatMessage::getCreateTime);
        Page<AIChatMessage> pageParam = new Page<>(page, size);
        return chatMapper.selectPage(pageParam, wrapper);
    }

    /** 保存消息 */
    public void saveMessage(Long sessionId, String role, String content, String imgUrl) {
        AIChatMessage msg = new AIChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setImgUrl(imgUrl);
        msg.setCreateTime(new Date());
        chatMapper.insert(msg);
    }

    /** 保存用户消息 */
    public void saveUserMessage(Long sessionId, String content, String imgUrl) {
        saveMessage(sessionId, "user", content, imgUrl);
        touchSession(sessionId);
    }

    /** 保存 AI 回复 */
    public void saveAssistantMessage(Long sessionId, String content) {
        saveMessage(sessionId, "assistant", content, null);
    }

    // ==================== 图片上传 ====================

    private static final String UPLOAD_DIR = "uploads/chat/";

    /** 上传聊天图片，返回可访问 URL */
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".png";
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            File dest = uploadPath.resolve(fileName).toFile();
            file.transferTo(dest);
            String url = "/" + UPLOAD_DIR + fileName;
            log.info("图片上传成功: {}", url);
            return url;
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }
}