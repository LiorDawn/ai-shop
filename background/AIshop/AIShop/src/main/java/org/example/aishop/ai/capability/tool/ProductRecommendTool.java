package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 个性化推荐工具
 *
 * 基于销量×新鲜度混合评分，为用户推荐热门商品。
 * 大模型调用后可基于推荐结果生成自然语言推荐语。
 */
@Component
public class ProductRecommendTool implements McpToolDefinition {

    @Autowired
    private ProductService productService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_PRODUCT_RECOMMEND;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        Long categoryId = params.getLong("categoryId", null);

        Page<ProductDTO> page = productService.recommendProducts(1, 10, categoryId);

        if (page.getRecords().isEmpty()) {
            return "暂无推荐商品，建议用户浏览商城首页。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("为您推荐以下热门商品：\n");
        for (int i = 0; i < page.getRecords().size(); i++) {
            ProductDTO p = page.getRecords().get(i);
            sb.append(i + 1).append(". ").append(p.getName())
                    .append(" — ¥").append(p.getPrice())
                    .append("（销量: ").append(p.getSales()).append("）")
                    .append(" [ID:").append(p.getId()).append("]\n");
        }
        return sb.toString();
    }
}