package org.example.aishop.controller.merchant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aishop.entity.chat.ChatMerchantMsg;
import org.example.aishop.entity.chat.ChatMerchantSession;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.entity.user.User;
import org.example.aishop.mapper.merchant.MerchantMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.service.chat.AIChatService;
import org.example.aishop.common.result.Result;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@Tag(name = "商家客服聊天", description = "商家与用户的客服会话")
@RestController
@RequestMapping("/merchant/chat")
public class MerchantChatController {

    @Autowired
    private AIChatService chatService;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取当前登录商家对应的商家表ID
     * UserHolder.getUserId() 返回的是 users 表的用户ID，
     * 而 chat_merchant_session.merchantId 存的是 merchant 表的商家ID，需要转换
     */
    private Long getMerchantTableId() {
        Long userId = UserHolder.getUserId();
        if (userId == null) return null;
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, userId));
        return merchant != null ? merchant.getId() : null;
    }

    @Operation(summary = "商家客服会话列表")
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        Long merchantId = getMerchantTableId();
        if (merchantId == null) return Result.fail("商家信息不存在");

        List<ChatMerchantSession> list = chatService.listMerchantSessionsByMerchant(merchantId);
        if (list.isEmpty()) return Result.success(Collections.emptyList());

        // 批量查询会话摘要（未读数 + 最后消息），一次查询替代 N 次
        List<Long> sessionIds = list.stream().map(ChatMerchantSession::getId).collect(Collectors.toList());
        Map<Long, Map<String, Object>> summaries = chatService.getMerchantSessionSummaries(sessionIds);

        // 批量查询用户昵称
        List<Long> userIds = list.stream().map(ChatMerchantSession::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> userNicknames = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                String name = "用户" + user.getId();
                if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
                    name = user.getNickname();
                } else if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                    name = user.getUsername();
                }
                userNicknames.put(user.getId(), name);
            }
        }

        List<Map<String, Object>> vos = list.stream().map(s -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", s.getId());
            vo.put("userId", s.getUserId());
            vo.put("merchantId", s.getMerchantId());
            vo.put("shopId", s.getShopId());
            vo.put("status", s.getStatus());
            vo.put("createTime", s.getCreateTime());
            vo.put("nickname", userNicknames.getOrDefault(s.getUserId(), "用户" + s.getUserId()));

            // 从批量查询结果中获取摘要
            Map<String, Object> summary = summaries.get(s.getId());
            if (summary != null) {
                vo.put("unreadCount", summary.get("unreadCount"));
                vo.put("lastMessage", summary.get("lastMessage"));
            } else {
                vo.put("unreadCount", 0);
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(vos);
    }

    @Operation(summary = "获取聊天消息", description = "按会话获取聊天记录")
    @GetMapping("/messages/{sessionId}")
    public Result<List<ChatMerchantMsg>> messages(@PathVariable Long sessionId) {
        Long merchantId = getMerchantTableId();
        Long userId = UserHolder.getUserId();
        if (merchantId == null || userId == null) return Result.fail("商家信息不存在");

        // 验证会话属于该商家
        ChatMerchantSession session = chatService.getMerchantSession(sessionId);
        if (session == null || !session.getMerchantId().equals(merchantId)) {
            return Result.fail("无权访问该会话");
        }

        // 标记已读：readerId 必须传用户表ID（与 send_id 字段一致），不能传商家表ID
        chatService.markMerchantMsgRead(sessionId, userId);

        return Result.success(chatService.listMerchantMsgs(sessionId));
    }

    @Operation(summary = "获取未读消息总数")
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long merchantId = getMerchantTableId();
        if (merchantId == null) return Result.fail("商家信息不存在");

        List<ChatMerchantSession> sessions = chatService.listMerchantSessionsByMerchant(merchantId);
        long totalUnread = 0;
        if (!sessions.isEmpty()) {
            List<Long> sessionIds = sessions.stream().map(ChatMerchantSession::getId).collect(Collectors.toList());
            Map<Long, Map<String, Object>> summaries = chatService.getMerchantSessionSummaries(sessionIds);
            for (Map<String, Object> summary : summaries.values()) {
                Object unread = summary.get("unreadCount");
                if (unread instanceof Number) {
                    totalUnread += ((Number) unread).longValue();
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", totalUnread > 99 ? 99 : totalUnread);
        return Result.success(result);
    }
}