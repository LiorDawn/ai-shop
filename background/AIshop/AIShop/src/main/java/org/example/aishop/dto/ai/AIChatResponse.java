package org.example.aishop.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI V1 聊天响应结果
 *
 * V1 版本非流式对话接口的响应体，包含会话 ID 和 AI 回复文本。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 聊天 V1 响应结果")
public class AIChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话 ID")
    private Long sessionId;

    @Schema(description = "AI 回复内容")
    private String reply;
}