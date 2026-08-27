package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.service.order.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 购物车删除工具
 *
 * 作用：从购物车中删除指定商品。
 * 参数：productId（必填）
 */
@Component
public class CartDeleteTool implements McpToolDefinition {

    @Autowired
    private CartService cartService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_CART_DELETE;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        Long productId = params.getLong("productId");

        cartService.delete(productId);

        return String.format("已成功从购物车中删除商品(ID:%d)", productId);
    }
}