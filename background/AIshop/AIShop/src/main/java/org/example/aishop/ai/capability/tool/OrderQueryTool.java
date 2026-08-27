package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.dto.OrderDTO;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.service.order.OrderService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单查询工具
 *
 * 查询当前用户的订单列表，支持按状态筛选。
 * 大模型调用后可基于订单数据回答用户关于订单的问题。
 */
@Component
public class OrderQueryTool implements McpToolDefinition {

    private static final String[] ORDER_STATUS = {"待付款", "待发货", "待收货", "已完成"};

    @Autowired
    private OrderService orderService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_ORDER_QUERY;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return "用户未登录，无法查询订单。";
        }

        Integer orderStatus = params.getInt("orderStatus", null);

        Page<OrderDTO> page = orderService.pageMyOrders(
                user.getId(), 1, 10, null, orderStatus);

        if (page.getRecords().isEmpty()) {
            String statusText = orderStatus != null ? ORDER_STATUS[orderStatus] + "的" : "";
            return "您暂时没有" + statusText + "订单。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您共有 ").append(page.getTotal()).append(" 个订单，展示最近 ").append(page.getRecords().size()).append(" 个：\n");
        for (int i = 0; i < page.getRecords().size(); i++) {
            OrderDTO o = page.getRecords().get(i);
            String statusText = o.getOrderStatus() != null && o.getOrderStatus() < ORDER_STATUS.length
                    ? ORDER_STATUS[o.getOrderStatus()] : "未知";
            sb.append(i + 1).append(". 订单号：").append(o.getOrderNo())
                    .append("，金额：¥").append(o.getActualPrice())
                    .append("，状态：").append(statusText)
                    .append(" [ID:").append(o.getId()).append("]\n");
        }
        return sb.toString();
    }
}