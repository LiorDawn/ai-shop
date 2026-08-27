package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.dto.AfterSaleDetailDTO;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.service.order.AfterSaleService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 售后详情查询工具
 *
 * 获取单条售后记录的完整信息（类型、金额、原因、物流等），
 * 大模型调用后可回答用户关于售后详情的具体问题。
 */
@Component
public class AfterSaleDetailTool implements McpToolDefinition {

    private static final String[] AUDIT_STATUS = {"待审核", "已通过", "已驳回", "待退货", "已完成"};

    @Autowired
    private AfterSaleService afterSaleService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_AFTERSALE_DETAIL;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return "用户未登录，无法查询售后详情。";
        }

        Long afterSaleId = params.getLong("afterSaleId");
        if (afterSaleId == null) {
            return "缺少售后ID，无法查询详情。";
        }

        AfterSaleDetailDTO detail = afterSaleService.getMyDetail(user.getId(), afterSaleId);
        if (detail == null) {
            return "售后记录不存在（ID: " + afterSaleId + "）。";
        }

        String statusText = detail.getAuditStatus() != null && detail.getAuditStatus() < AUDIT_STATUS.length
                ? AUDIT_STATUS[detail.getAuditStatus()] : "未知";
        String typeText = detail.getType() == 0 ? "仅退款" : "退货退款";

        StringBuilder sb = new StringBuilder();
        sb.append("售后详情：\n");
        sb.append("类型：").append(typeText).append("\n");
        sb.append("金额：¥").append(detail.getAmount()).append("\n");
        sb.append("状态：").append(statusText).append("\n");
        if (detail.getReason() != null) {
            sb.append("原因：").append(detail.getReason()).append("\n");
        }
        if (detail.getDescription() != null) {
            sb.append("描述：").append(detail.getDescription()).append("\n");
        }

        return sb.toString();
    }
}