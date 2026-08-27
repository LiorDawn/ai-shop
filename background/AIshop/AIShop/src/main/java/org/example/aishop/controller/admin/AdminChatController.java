package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.entity.chat.ChatPlatformMsg;
import org.example.aishop.entity.chat.ChatPlatformSession;
import org.example.aishop.entity.user.User;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.service.chat.AIChatService;
import org.example.aishop.common.result.Result;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "管理端客服聊天", description = "平台客服会话管理")
@RestController
@RequestMapping("/admin/chat")
public class AdminChatController {

    @Autowired
    private AIChatService chatService;
    @Autowired
    private UserMapper userMapper;

    @Operation(summary = "获取平台客服会话列表")
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        Long adminId = UserHolder.getUserId();
        if (adminId == null) return Result.fail("请先登录");

        List<ChatPlatformSession> list = chatService.listPlatformSessions();
        List<Map<String, Object>> vos = list.stream().map(s -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", s.getId());
            vo.put("userId", s.getUserId());
            vo.put("adminId", s.getAdminId());
            vo.put("status", s.getStatus());
            vo.put("createTime", s.getCreateTime());

            // 附带用户昵称信息（nickname → username → 兜底）
            User user = userMapper.selectById(s.getUserId());
            String userName = "用户" + s.getUserId();
            if (user != null) {
                if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
                    userName = user.getNickname();
                } else if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                    userName = user.getUsername();
                }
            }
            vo.put("nickname", userName);

            // 统计未读消息数
            List<ChatPlatformMsg> msgs = chatService.listPlatformMsgs(s.getId());
            long unread = msgs.stream().filter(m -> m.getIsRead() == 0 && m.getSendType() != 2).count();
            vo.put("unreadCount", unread > 99 ? 99 : unread);

            // 最后一条消息
            if (!msgs.isEmpty()) {
                vo.put("lastMessage", msgs.get(msgs.size() - 1).getContent());
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(vos);
    }

    @Operation(summary = "获取会话消息", description = "按会话获取平台客服聊天记录")
    @GetMapping("/messages/{sessionId}")
    public Result<List<ChatPlatformMsg>> messages(@PathVariable Long sessionId) {
        Long adminId = UserHolder.getUserId();
        if (adminId == null) return Result.fail("请先登录");

        // 验证会话
        ChatPlatformSession session = chatService.getPlatformSession(sessionId);
        if (session == null) return Result.fail("会话不存在");

        // 标记已读
        chatService.markPlatformMsgRead(sessionId, adminId);

        return Result.success(chatService.listPlatformMsgs(sessionId));
    }

    /** 获取用户信息 */
    @GetMapping("/user-info/{userId}")
    public Result<Map<String, Object>> userInfo(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");

        Map<String, Object> vo = new HashMap<>();
        vo.put("username", user.getUsername());
        vo.put("nickname", user.getNickname());
        // 统一返回显示名：nickname → username → 兜底
        String displayName = "用户" + userId;
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            displayName = user.getNickname();
        } else if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            displayName = user.getUsername();
        }
        vo.put("displayName", displayName);
        return Result.success(vo);
    }

    @Operation(summary = "获取未读消息总数")
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long adminId = UserHolder.getUserId();
        if (adminId == null) return Result.fail("请先登录");

        List<ChatPlatformSession> sessions = chatService.listPlatformSessions();
        long totalUnread = 0;
        for (ChatPlatformSession s : sessions) {
            List<ChatPlatformMsg> msgs = chatService.listPlatformMsgs(s.getId());
            totalUnread += msgs.stream().filter(m -> m.getIsRead() == 0 && m.getSendType() != 2).count();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", totalUnread > 99 ? 99 : totalUnread);
        return Result.success(result);
    }
}