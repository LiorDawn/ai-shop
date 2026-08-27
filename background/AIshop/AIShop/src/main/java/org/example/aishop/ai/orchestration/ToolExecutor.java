package org.example.aishop.ai.orchestration;

import cn.hutool.json.JSONObject;
import org.example.aishop.dto.ai.ToolCallResult;

/**
 * 工具执行层接口 — MCP 工具调用、校验、重试、异常处理
 *
 * 职责：
 * 1. 根据工具名匹配 MCP 工具并执行
 * 2. 统一异常捕获 → 封装失败结果
 * 3. 工具结果摘要压缩
 * 4. 同工具重复调用检测
 */
public interface ToolExecutor {

    /**
     * 执行 MCP 工具
     *
     * @param toolName 工具名称
     * @param params   工具参数
     * @return 执行结果
     */
    ToolCallResult execute(String toolName, JSONObject params);

    /**
     * 对工具执行结果做摘要压缩（超过阈值则截断）
     */
    String summarizeResult(ToolCallResult result);
}