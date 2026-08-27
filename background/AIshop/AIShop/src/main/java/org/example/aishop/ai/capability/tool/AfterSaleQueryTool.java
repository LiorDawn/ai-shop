package org.example.aishop.ai.capability.tool;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.ai.capability.mcp.McpToolDefinition;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.dto.AfterSaleDTO;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.service.order.AfterSaleService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 售后查询工具
 *
 * 查询当前用户的售后列表，支持按审核状态筛选。
 * 大模型调用后可告知用户售后进度。
 */
@Component
public class AfterSaleQueryTool implements McpToolDefinition {

    private static final String[] AUDIT_STATUS = {"待审核", "已通过", "已驳回", "待退货", "已完成"};

    @Autowired
    private AfterSaleService afterSaleService;

    @Override
    public String getToolName() {
        return ToolCallSchema.TOOL_AFTERSALE_QUERY;
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return "用户未登录，无法查询售后。";
        }

        Integer auditStatus = params.getInt("auditStatus", null);

        Page<AfterSaleDTO> page = afterSaleService.pageMyAfterSales(
                user.getId(), 1, 10, auditStatus);

        if (page.getRecords().isEmpty()) {
            return "您暂时没有售后记录。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您共有 ").append(page.getTotal()).append(" 条售后记录：\n");
        for (int i = 0; i < page.getRecords().size(); i++) {
            AfterSaleDTO a = page.getRecords().get(i);
            String statusText = a.getAuditStatus() != null && a.getAuditStatus() < AUDIT_STATUS.length
                    ? AUDIT_STATUS[a.getAuditStatus()] : "未知";
            String typeText = a.getType() == 0 ? "仅退款" : "退货退款";
            sb.append(i + 1).append(". ").append(typeText)
                    .append("，金额：¥").append(a.getAmount())
                    .append("，状态：").append(statusText);
            if (a.getReason() != null) {
                sb.append("，原因：").append(a.getReason());
            }
            sb.append(" [ID:").append(a.getId()).append("]\n");
        }
        return sb.toString();
    }
}