package org.example.aishop.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具执行结果统一结构体
 *
 * 每次工具执行完成后，封装为标准化结果，追加到对话上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallResult {

    /** 工具名称 */
    private String toolName;

    /** 执行是否成功 */
    private boolean success;

    /** 执行结果文本（成功时描述操作效果，失败时描述错误原因） */
    private String message;

    /** 执行详情（结构化数据，如查询结果列表） */
    private Object data;

    /** 执行耗时毫秒 */
    private long elapsedMs;

    /** 发生异常时的异常类型 */
    private String errorType;

    /**
     * 构建成功结果
     */
    public static ToolCallResult success(String toolName, String message, Object data, long elapsedMs) {
        return ToolCallResult.builder()
                .toolName(toolName)
                .success(true)
                .message(message)
                .data(data)
                .elapsedMs(elapsedMs)
                .build();
    }

    /**
     * 构建失败结果
     */
    public static ToolCallResult failure(String toolName, String message, String errorType, long elapsedMs) {
        return ToolCallResult.builder()
                .toolName(toolName)
                .success(false)
                .message(message)
                .errorType(errorType)
                .elapsedMs(elapsedMs)
                .build();
    }
}