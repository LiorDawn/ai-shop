package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.dto.CartItemVO;
import org.example.aishop.service.order.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 购物车查询工具
 *
 * 作用：查询用户购物车中所有商品，返回商品名称、价格、数量、选中状态等。
 * 大模型调用此工具后，可以基于购物车真实数据给用户建议。
 */
@Component
public class CartQueryTool implements McpToolDefinition {

    @Autowired
    private CartService cartService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_CART_QUERY;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        List<CartItemVO> items = cartService.listCart();

        if (items == null || items.isEmpty()) {
            return "购物车是空的，还没有任何商品。建议用户去商城逛逛添加商品。";
        }

        StringBuilder sb = new StringBuilder("购物车当前有 ");
        int totalCount = 0;
        int checkedCount = 0;

        for (CartItemVO item : items) {
            sb.append("\n- ").append(item.getProductName())
                    .append("，价格：¥").append(item.getPrice())
                    .append("，数量：").append(item.getNum())
                    .append("，店铺：").append(item.getShopName());
            if (item.getChecked() != null && item.getChecked() == 1) {
                sb.append("（已勾选）");
                checkedCount++;
            }
            totalCount += item.getNum();
        }

        sb.insert(0, "购物车共 " + items.size() + " 种商品，" + totalCount + " 件，其中已勾选 " + checkedCount + " 种。");
        return sb.toString();
    }
}