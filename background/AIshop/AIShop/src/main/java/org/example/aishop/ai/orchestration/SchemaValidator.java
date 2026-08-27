package org.example.aishop.ai.orchestration;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.example.aishop.ai.support.OperationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Schema 校验器 — 校验大模型返回的 JSON 是否符合工具调用格式
 *
 * 校验规则：
 * 1. JSON 可解析（非空、非纯文本）
 * 2. 包含 toolName、parameters、needContinueTool 三个字段
 * 3. toolName 在可用工具列表中（或为 "none"）
 * 4. 必填参数存在
 *
 * 校验失败时返回错误信息，由编排层决定是否重试。
 */
@Component
public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    @Autowired
    private OperationLogger operationLogger;

    /**
     * 校验大模型输出的 JSON 结构
     *
     * @param rawOutput 大模型原始输出文本
     * @return 校验结果（OK 或 ERROR + 原因）
     */
    public ValidationResult validate(String rawOutput) {
        long start = System.currentTimeMillis();

        // 1. 空值检查
        if (rawOutput == null || rawOutput.isBlank()) {
            return fail("大模型返回为空", rawOutput, start);
        }

        // 2. 清理 markdown 代码块标记（```json ... ```）
        String cleaned = cleanMarkdown(rawOutput);

        // 3. JSON 解析
        JSONObject json;
        try {
            json = JSONUtil.parseObj(cleaned);
        } catch (Exception e) {
            log.warn("JSON 解析失败: raw={}", rawOutput.substring(0, Math.min(200, rawOutput.length())));
            return fail("JSON 解析失败: " + e.getMessage(), rawOutput, start);
        }

        // 4. 必填字段检查
        if (!json.containsKey(ToolCallSchema.FIELD_TOOL_NAME)) {
            return fail("缺少必填字段: toolName", rawOutput, start);
        }
        if (!json.containsKey(ToolCallSchema.FIELD_NEED_CONTINUE)) {
            return fail("缺少必填字段: needContinueTool", rawOutput, start);
        }

        // 5. toolName 合法性检查
        String toolName = json.getStr(ToolCallSchema.FIELD_TOOL_NAME);
        if (!isValidToolName(toolName)) {
            return fail("无效的 toolName: " + toolName + "，可用值: " + getAvailableToolNames(), rawOutput, start);
        }

        // 6. 必填参数检查
        if (!"none".equals(toolName)) {
            JSONObject params = json.getJSONObject(ToolCallSchema.FIELD_PARAMETERS);
            List<ToolCallSchema.ParamDef> paramDefs = ToolCallSchema.getToolParamDefs().get(toolName);
            if (paramDefs != null) {
                for (ToolCallSchema.ParamDef def : paramDefs) {
                    if (def.required() && (params == null || !params.containsKey(def.name()))) {
                        return fail("工具 " + toolName + " 缺少必填参数: " + def.name(), rawOutput, start);
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Schema 校验通过: toolName={}, elapsed={}ms", toolName, elapsed);
        return ValidationResult.ok(json, cleaned);
    }

    /**
     * 清理 markdown 代码块标记
     */
    private String cleanMarkdown(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf("\n");
            if (start > 0) {
                cleaned = cleaned.substring(start + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }
        return cleaned.trim();
    }

    private boolean isValidToolName(String name) {
        return "none".equals(name)
                // 购物车工具
                || ToolCallSchema.TOOL_CART_QUERY.equals(name)
                || ToolCallSchema.TOOL_CART_ADD.equals(name)
                || ToolCallSchema.TOOL_CART_DELETE.equals(name)
                || ToolCallSchema.TOOL_CART_UPDATE_NUM.equals(name)
                || ToolCallSchema.TOOL_CART_CHECK_ALL.equals(name)
                // 商品工具
                || ToolCallSchema.TOOL_PRODUCT_QUERY.equals(name)
                || ToolCallSchema.TOOL_PRODUCT_RECOMMEND.equals(name)
                || ToolCallSchema.TOOL_PRODUCT_DETAIL.equals(name)
                // 订单工具
                || ToolCallSchema.TOOL_ORDER_QUERY.equals(name)
                || ToolCallSchema.TOOL_ORDER_DETAIL.equals(name)
                // 售后工具
                || ToolCallSchema.TOOL_AFTERSALE_QUERY.equals(name)
                || ToolCallSchema.TOOL_AFTERSALE_DETAIL.equals(name);
    }

    private String getAvailableToolNames() {
        return String.join(", ",
                ToolCallSchema.TOOL_CART_QUERY,
                ToolCallSchema.TOOL_CART_ADD,
                ToolCallSchema.TOOL_CART_DELETE,
                ToolCallSchema.TOOL_CART_UPDATE_NUM,
                ToolCallSchema.TOOL_CART_CHECK_ALL,
                ToolCallSchema.TOOL_PRODUCT_QUERY,
                ToolCallSchema.TOOL_PRODUCT_RECOMMEND,
                ToolCallSchema.TOOL_PRODUCT_DETAIL,
                ToolCallSchema.TOOL_ORDER_QUERY,
                ToolCallSchema.TOOL_ORDER_DETAIL,
                ToolCallSchema.TOOL_AFTERSALE_QUERY,
                ToolCallSchema.TOOL_AFTERSALE_DETAIL,
                "none");
    }

    private ValidationResult fail(String reason, String rawOutput, long start) {
        long elapsed = System.currentTimeMillis() - start;
        operationLogger.logSchemaValidation(false, reason, elapsed);
        return ValidationResult.error(reason);
    }

    /**
     * 校验结果
     */
    public record ValidationResult(boolean valid, JSONObject json, String cleanedJson, String errorMessage) {
        public static ValidationResult ok(JSONObject json, String cleanedJson) {
            return new ValidationResult(true, json, cleanedJson, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, null, null, message);
        }
    }
}