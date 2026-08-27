package org.example.aishop.service.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AI 内容服务：商品文案生成、智能审核、工单分类
 */
@Service
public class AIContentService {

    private static final Logger log = LoggerFactory.getLogger(AIContentService.class);

    @Autowired
    @Qualifier("chatLanguageModel")
    private ChatLanguageModel chatModel;

    // ==================== 1. 商品文案生成 ====================

    /**
     * 根据商品名称和分类生成商品描述、卖点文案
     */
    public String generateProductDescription(String productName, String categoryName, String imageUrl) {
        String prompt = buildProductDescriptionPrompt(productName, categoryName, imageUrl);
        try {
            String result = chatModel.generate(prompt);
            return cleanResult(result);
        } catch (Exception e) {
            log.error("AI 生成商品描述失败: " + e.getMessage());
            return null;
        }
    }

    private String buildProductDescriptionPrompt(String productName, String categoryName, String imageUrl) {
        return String.format("""
            你是一个电商文案专家。请为以下商品生成一段吸引人的描述和卖点文案。
            
            商品名称：%s
            商品分类：%s
            
            请按以下格式输出（200字以内）：
            【商品描述】一段简洁的商品介绍
            【核心卖点】3个卖点，用·开头
            【推荐语】一句促销推荐语
            
            语言风格：专业、有吸引力、适合电商展示。
            """, productName, categoryName != null ? categoryName : "通用");
    }

    // ==================== 2. AI 智能审核 ====================

    /**
     * 审核内容是否合规
     * @return null 表示通过，非空字符串表示违规原因
     */
    public String reviewContent(String content, String contentType) {
        if (content == null || content.trim().isEmpty()) return null;

        String prompt = buildReviewPrompt(content, contentType);
        try {
            String result = chatModel.generate(prompt);
            result = result.trim();
            if (result.startsWith("PASS") || result.contains("通过")) {
                return null;
            }
            return result.length() > 100 ? result.substring(0, 100) : result;
        } catch (Exception e) {
            log.error("AI 内容审核失败: " + e.getMessage());
            return null; // 审核失败时放行，避免阻塞正常流程
        }
    }

    private String buildReviewPrompt(String content, String contentType) {
        return String.format("""
            你是一个电商内容审核员。请审核以下%s内容是否违规。
            
            内容：%s
            
            违规类型包括：色情低俗、暴力恐怖、政治敏感、广告骚扰、虚假信息、辱骂攻击。
            
            如果内容合规，请只回复：PASS
            如果内容违规，请回复：REJECT: 违规原因（简短说明）
            
            只回复 PASS 或 REJECT: 开头的内容，不要其他文字。
            """, contentType, content);
    }

    // ==================== 3. 智能客服工单分类 ====================

    /**
     * 分析客服消息内容，返回分类标签
     * @return 分类：商品咨询 / 订单问题 / 售后申请 / 投诉建议 / 其他
     */
    public String classifyTicket(String message) {
        if (message == null || message.trim().isEmpty()) return "其他";

        String prompt = buildClassifyPrompt(message);
        try {
            String result = chatModel.generate(prompt);
            result = result.trim();
            // 标准化分类结果
            if (result.contains("商品咨询")) return "商品咨询";
            if (result.contains("订单问题")) return "订单问题";
            if (result.contains("售后")) return "售后申请";
            if (result.contains("投诉") || result.contains("建议")) return "投诉建议";
            return "其他";
        } catch (Exception e) {
            log.error("AI 工单分类失败: " + e.getMessage());
            return "其他";
        }
    }

    private String buildClassifyPrompt(String message) {
        return String.format("""
            你是一个客服工单分类助手。请将以下用户消息分类到以下类别之一：
            - 商品咨询：询问商品信息、价格、库存、规格等
            - 订单问题：查询订单状态、物流、修改订单等
            - 售后申请：退换货、退款、质量问题等
            - 投诉建议：对服务不满意、提出改进建议等
            - 其他：不属于以上类别
            
            用户消息：%s
            
            请只回复分类名称（如：商品咨询），不要回复其他内容。
            """, message);
    }

    private String cleanResult(String result) {
        if (result == null) return null;
        return result.replaceAll("```[\\s\\S]*?```", "").trim();
    }
}