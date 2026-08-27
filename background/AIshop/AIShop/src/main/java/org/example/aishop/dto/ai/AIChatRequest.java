package org.example.aishop.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI V1 聊天请求参数
 *
 * V1 版本接口请求体，用于非流式 /send 接口。
 * 相比 V2 少了 toolEnabled 字段，仅支持基础对话 + RAG。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 聊天 V1 请求参数")
public class AIChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话 ID（首次为空，后端自动创建）")
    private Long sessionId;

    @Schema(description = "用户消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "图片访问路径（多模态模式）")
    private String imgUrl;
}