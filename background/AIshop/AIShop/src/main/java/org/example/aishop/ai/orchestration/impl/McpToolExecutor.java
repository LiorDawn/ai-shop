package org.example.aishop.ai.orchestration.impl;

import cn.hutool.json.JSONObject;
import org.example.aishop.dto.ai.ToolCallResult;
import org.example.aishop.ai.capability.mcp.McpToolRegistry;
import org.example.aishop.ai.orchestration.ToolExecutor;
import org.example.aishop.ai.support.OperationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MCP 工具执行器 — 调用 + 异常捕获 + 结果摘要压缩
 */
@Component
public class McpToolExecutor implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(McpToolExecutor.class);
    private static final int MAX_RESULT_CHARS = 500;

    @Autowired
    private McpToolRegistry mcpToolRegistry;

    @Autowired
    private OperationLogger operationLogger;

    @Override
    public ToolCallResult execute(String toolName, JSONObject params) {
        ToolCallResult result = mcpToolRegistry.execute(toolName, params);
        operationLogger.logToolExecution(toolName, result.isSuccess(), result.getMessage());
        return result;
    }

    @Override
    public String summarizeResult(ToolCallResult result) {
        String text = String.format("[工具执行结果] %s: %s — %s",
                result.getToolName(),
                result.isSuccess() ? "成功" : "失败",
                result.getMessage());

        if (text.length() > MAX_RESULT_CHARS) {
            return text.substring(0, MAX_RESULT_CHARS)
                    + "\n...（结果过长已截断，共 " + text.length() + " 字符）";
        }
        return text;
    }
}