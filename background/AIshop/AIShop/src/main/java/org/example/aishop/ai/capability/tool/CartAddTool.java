package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.service.order.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 购物车添加工具
 *
 * 作用：将指定商品添加到用户购物车。
 * 参数：productId（必填）、skuId（可选）、num（可选，默认 1）
 */
@Component
public class CartAddTool implements McpToolDefinition {

    @Autowired
    private CartService cartService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_CART_ADD;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        Long productId = params.getLong("productId");
        Long skuId = params.getLong("skuId");
        Integer num = params.getInt("num", 1);

        if (num <= 0) num = 1;

        cartService.add(productId, skuId, num);

        return String.format("已成功将商品(ID:%d)加入购物车，数量：%d 件", productId, num);
    }
}