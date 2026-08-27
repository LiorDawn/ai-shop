package org.example.aishop.ai.capability.mcp;

import cn.hutool.json.JSONObject;

/**
 * MCP 工具定义接口
 *
 * 所有可被大模型调用的工具都必须实现此接口。
 * 每个工具需提供：
 * 1. 工具名称（与 Schema 中 toolName 对应）
 * 2. 执行方法（接收 JSON 参数，返回执行结果文本）
 *
 * 工具内部直接通过 UserHolder 获取当前用户上下文。
 */
public interface McpToolDefinition {

    /**
     * 工具唯一名称
     * 必须与 ToolCallSchema 中定义的 toolName 一致
     */
    String getToolName();

    /**
     * 执行工具
     *
     * @param params 已校验并补齐的参数 JSON
     * @return 执行结果文本（成功时描述操作效果，失败时抛异常）
     * @throws Exception 业务异常会自动被 McpToolRegistry 捕获并封装
     */
    String execute(JSONObject params) throws Exception;
}