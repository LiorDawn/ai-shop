package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.service.order.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 购物车修改数量工具
 *
 * 作用：修改购物车中指定商品的数量。
 * 参数：productId（必填）、num（必填，新数量）
 */
@Component
public class CartUpdateNumTool implements McpToolDefinition {

    @Autowired
    private CartService cartService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_CART_UPDATE_NUM;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        Long productId = params.getLong("productId");
        Integer num = params.getInt("num");

        if (num == null || num < 1) {
            throw new IllegalArgumentException("数量必须大于 0");
        }

        cartService.updateNum(productId, num);

        return String.format("已将商品(ID:%d)的数量修改为 %d 件", productId, num);
    }
}