package org.example.aishop.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * V2 版 AI 聊天请求参数
 *
 * 相比 V1 新增 toolEnabled 字段，控制是否启用 MCP 工具调用能力。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 聊天 V2 请求参数")
public class ChatRequestV2 implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话 ID（首次为空，后端自动创建）")
    private Long sessionId;

    @Schema(description = "用户消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "图片访问路径（多模态模式）")
    private String imgUrl;

    @Schema(description = "是否启用 RAG 商品知识库检索增强")
    private Boolean ragEnabled = true;

    @Schema(description = "是否启用 MCP 工具调用（购物车增删改查）")
    private Boolean toolEnabled = true;
}