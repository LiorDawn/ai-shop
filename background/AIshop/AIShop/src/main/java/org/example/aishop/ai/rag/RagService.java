package org.example.aishop.ai.rag;

import org.example.aishop.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 检索增强生成服务
 * 负责编排检索 + 增强流程，为 AI 对话提供上下文
 */
@Service
public class RagService {
    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    @Autowired
    private ProductKnowledgeBase knowledgeBase;

    @Autowired
    private AiProperties aiProperties;

    /**
     * 根据用户查询，检索相关商品并构建增强上下文
     *
     * @param userQuery 用户查询文本
     * @return 增强后的上下文文本（可直接注入到 AI prompt 中），无结果时返回空字符串
     */
    public String buildRagContext(String userQuery) {
        if (!aiProperties.getRag().isEnabled()) {
            return "";
        }

        try {
            List<ProductKnowledgeBase.ProductSearchResult> results = knowledgeBase.search(userQuery);
            if (results.isEmpty()) {
                return "";
            }

            return formatContext(results);
        } catch (Exception e) {
            log.error("RAG 检索失败: " + e.getMessage());
            return "";
        }
    }

    /**
     * 判断用户查询是否适合使用 RAG
     * 只对商品推荐、咨询类问题启用 RAG
     */
    public boolean shouldUseRag(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return false;
        }

        String lower = userQuery.toLowerCase();
        // 商品相关关键词
        String[] productKeywords = {
                "推荐", "买", "卖", "商品", "产品", "价格", "多少钱", "便宜",
                "质量", "好用", "怎么样", "有没有", "哪个好", "选择", "比较",
                "手机", "电脑", "耳机", "衣服", "鞋子", "手表", "充电",
                "推荐商品", "购物", "什么牌子", "品牌", "款式", "类型",
                "热销", "新品", "优惠", "折扣", "促销"
        };

        for (String keyword : productKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 格式化检索结果为上下文文本
     */
    private String formatContext(List<ProductKnowledgeBase.ProductSearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【以下是商城相关商品信息，供你参考推荐给用户】\n");

        for (int i = 0; i < results.size(); i++) {
            ProductKnowledgeBase.ProductSearchResult r = results.get(i);
            sb.append((i + 1)).append(". ");
            sb.append("商品ID：").append(r.getProductId());
            sb.append("，商品名称：").append(r.getName());
            sb.append("，价格：¥").append(r.getPrice());
            if (r.getDescription() != null && !r.getDescription().isEmpty()) {
                // 截断过长描述
                String desc = r.getDescription().length() > 100
                        ? r.getDescription().substring(0, 100) + "..."
                        : r.getDescription();
                sb.append("，描述：").append(desc);
            }
            if (r.getSales() != null && r.getSales() > 0) {
                sb.append("，销量：").append(r.getSales()).append("件");
            }
            sb.append("（相关度：").append(String.format("%.0f%%", r.getSimilarity() * 100)).append("）");
            sb.append("\n");
        }

        sb.append("请基于以上真实商品信息，友好地向用户推荐。如果用户问题与商品无关，请忽略以上信息。");
        return sb.toString();
    }
}