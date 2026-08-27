package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 商品详情查询工具
 *
 * 获取单个商品的完整信息（名称、价格、描述、SKU、库存等），
 * 大模型调用后可基于详情给用户做购买建议。
 */
@Component
public class ProductDetailTool implements McpToolDefinition {

    @Autowired
    private ProductService productService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_PRODUCT_DETAIL;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        Long productId = params.getLong("productId");
        if (productId == null) {
            return "缺少商品ID，无法查询详情。";
        }

        ProductDTO product = productService.getProductById(productId);
        if (product == null) {
            return "商品不存在或已下架（ID: " + productId + "）。";
        }

        return String.format(
                "商品详情：\n名称：%s\n价格：¥%s\n描述：%s\n销量：%d\n库存：%d\n状态：%s",
                product.getName(),
                product.getPrice(),
                product.getDescription() != null ? product.getDescription() : "暂无描述",
                product.getSales() != null ? product.getSales() : 0,
                product.getStock() != null ? product.getStock() : 0,
                product.getStatus() == 1 ? "在售" : "已下架"
        );
    }
}