package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 商品搜索工具
 *
 * 支持按关键词、分类搜索商品，大模型调用后可基于真实商品数据给用户推荐。
 */
@Component
public class ProductQueryTool implements McpToolDefinition {

    @Autowired
    private ProductService productService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_PRODUCT_QUERY;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        String keyword = params.getStr("keyword", "");
        Long categoryId = params.getLong("categoryId", null);
        Integer sort = params.getInt("sort", 0);

        IPage<ProductDTO> page = productService.pageProducts(
                1, 10, keyword, categoryId, null, 1, null);

        if (page.getRecords().isEmpty()) {
            return "未找到相关商品，建议用户尝试其他关键词或浏览分类。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(page.getTotal()).append(" 款商品，为您展示前 ").append(page.getRecords().size()).append(" 款：\n");
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