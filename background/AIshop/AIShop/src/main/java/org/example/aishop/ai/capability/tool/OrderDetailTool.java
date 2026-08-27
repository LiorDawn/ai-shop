package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.dto.OrderDetailDTO;
import org.example.aishop.service.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单详情查询工具
 *
 * 获取单个订单的完整信息（商品明细、物流、金额等），
 * 大模型调用后可回答用户关于订单详情的具体问题。
 */
@Component
public class OrderDetailTool implements McpToolDefinition {

    @Autowired
    private OrderService orderService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_ORDER_DETAIL;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        Long orderId = params.getLong("orderId");
        if (orderId == null) {
            return "缺少订单ID，无法查询详情。";
        }

        OrderDetailDTO detail = orderService.getOrderDetail(orderId);
        if (detail == null) {
            return "订单不存在（ID: " + orderId + "）。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("订单详情：\n");
        sb.append("订单号：").append(detail.getOrderNo()).append("\n");
        sb.append("下单时间：").append(detail.getCreateTime()).append("\n");
        sb.append("订单金额：¥").append(detail.getActualPrice()).append("\n");
        sb.append("商品明细：\n");

        if (detail.getItems() != null) {
            for (OrderDetailDTO.OrderItemDTO item : detail.getItems()) {
                sb.append("  - ").append(item.getProductName())
                        .append(" ×").append(item.getNum())
                        .append("，¥").append(item.getPrice()).append("/件\n");
            }
        }

        if (detail.getLogistics() != null && !detail.getLogistics().isEmpty()) {
            sb.append("物流信息：").append(detail.getLogistics()).append("\n");
        }

        return sb.toString();
    }
}