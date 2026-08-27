package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.service.order.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 购物车全选/全不选工具
 *
 * 作用：批量操作购物车所有商品的选中状态。
 * 参数：checked（必填，1=全选，0=全不选）
 */
@Component
public class CartCheckAllTool implements McpToolDefinition {

    @Autowired
    private CartService cartService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_CART_CHECK_ALL;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        Integer checked = params.getInt("checked");

        if (checked == null || (checked != 0 && checked != 1)) {
            throw new IllegalArgumentException("checked 参数必须为 0 或 1");
        }

        cartService.checkAll(checked);

        return checked == 1 ? "已全选购物车中的所有商品" : "已取消全选购物车中的所有商品";
    }
}