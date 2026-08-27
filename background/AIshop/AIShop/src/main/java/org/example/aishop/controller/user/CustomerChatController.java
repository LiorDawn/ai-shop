package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import org.example.aishop.service.ai.AIContentService;
import org.example.aishop.entity.chat.ChatMerchantMsg;
import org.example.aishop.entity.chat.ChatMerchantSession;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.common.result.Result;
import org.example.aishop.mapper.merchant.MerchantMapper;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.service.chat.AIChatService;
import org.example.aishop.util.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer-chat")
public class CustomerChatController {

    @Autowired
    private AIChatService chatService;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private AIContentService aiContentService;

    @Operation(summary = "获取聊天历史", description = "合并商家 + 平台客服的会话列表")
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history() {
        Long userId = UserHolder.getUserId();
        if (userId == null) return Result.fail("请先登录");

        List<Map<String, Object>> result = new ArrayList<>();

        // 商家聊天会话 - 按 merchantId 去重，只保留最新一条
        List<ChatMerchantSession> merchantSessions = chatService.listMerchantSessionsByUser(userId);
        Map<Long, ChatMerchantSession> latestMerchantSession = new LinkedHashMap<>();
        for (ChatMerchantSession s : merchantSessions) {
            latestMerchantSession.merge(s.getMerchantId(), s, (a, b) -> {
                Date ta = a.getCreateTime();
                Date tb = b.getCreateTime();
                if (ta == null) return b;
                if (tb == null) return a;
                return ta.after(tb) ? a : b;
            });
        }

        if (!latestMerchantSession.isEmpty()) {
            List<ChatMerchantSession> sessions = new ArrayList<>(latestMerchantSession.values());

            // 批量查询会话摘要（最后消息），一次查询替代 N 次
            List<Long> sessionIds = sessions.stream().map(ChatMerchantSession::getId).collect(Collectors.toList());
            Map<Long, Map<String, Object>> summaries = chatService.getMerchantSessionSummaries(sessionIds);

            // 批量查询店铺信息
            List<Long> shopIds = sessions.stream().map(ChatMerchantSession::getShopId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, String> shopNameMap = new HashMap<>();
            if (!shopIds.isEmpty()) {
                List<Shop> shops = shopMapper.selectBatchIds(shopIds);
                for (Shop shop : shops) {
                    shopNameMap.put(shop.getId(), shop.getShopName());
                }
            }

            // 批量查询商家信息
            List<Long> merchantIds = sessions.stream().map(ChatMerchantSession::getMerchantId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, String> merchantNameMap = new HashMap<>();
            if (!merchantIds.isEmpty()) {
                List<Merchant> merchants = merchantMapper.selectBatchIds(merchantIds);
                for (Merchant m : merchants) {
                    merchantNameMap.put(m.getId(), m.getMerchantName());
                }
            }

            for (ChatMerchantSession s : sessions) {
                Map<String, Object> vo = new HashMap<>();
                vo.put("id", s.getId());
                vo.put("type", "merchant");
                vo.put("shopId", s.getShopId());
                vo.put("merchantId", s.getMerchantId());
                vo.put("status", s.getStatus());
                vo.put("createTime", s.getCreateTime());

                // 查询店铺名称
                String shopName = null;
                if (s.getShopId() != null) {
                    shopName = shopNameMap.get(s.getShopId());
                }
                if (shopName == null && s.getMerchantId() != null) {
                    // 通过 merchantId 找店铺
                    shopName = merchantNameMap.get(s.getMerchantId());
                }
                vo.put("shopName", shopName != null ? shopName : "店铺客服");

                if ("平台客服".equals(shopName)) {
                    continue;
                }

                // 从批量查询结果中获取最后消息
                Map<String, Object> summary = summaries.get(s.getId());
                if (summary != null) {
                    vo.put("lastMessage", summary.get("lastMessage"));
                }
                result.add(vo);
            }
        }

        // 按创建时间倒序
        result.sort((a, b) -> {
            Date ta = (Date) a.get("createTime");
            Date tb = (Date) b.get("createTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        return Result.success(result);
    }

    /** 获取会话消息 */
    @GetMapping("/messages/{sessionId}")
    public Result<List<Map<String, Object>>> messages(
            @PathVariable Long sessionId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) return Result.fail("请先登录");

        List<Map<String, Object>> result = new ArrayList<>();

        ChatMerchantSession session = chatService.getMerchantSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.fail("无权访问该会话");
        }
        chatService.markMerchantMsgRead(sessionId, userId);
        List<ChatMerchantMsg> msgs = chatService.listMerchantMsgs(sessionId);
        for (ChatMerchantMsg m : msgs) {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", m.getId());
            vo.put("sessionId", m.getSessionId());
            vo.put("sendType", m.getSendType());
            vo.put("content", m.getContent());
            vo.put("createTime", m.getCreateTime());
            result.add(vo);
        }

        return Result.success(result);
    }

    @Operation(summary = "AI 智能工单分类", description = "根据用户消息内容，AI 自动分类为：商品咨询/订单问题/售后申请/投诉建议/其他")
    @PostMapping("/classify")
    public Result<Map<String, String>> classify(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Result.fail("消息不能为空");
        }
        String category = aiContentService.classifyTicket(message);
        Map<String, String> result = new HashMap<>();
        result.put("category", category);
        return Result.success("分类成功", result);
    }
}