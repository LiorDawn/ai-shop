package org.example.aishop.ai.orchestration;

import cn.hutool.json.JSONObject;
import org.example.aishop.ai.orchestration.schema.ToolCallSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 参数填充器 — 自动补全缺失参数为默认值
 *
 * 当大模型返回的工具调用 JSON 中缺少可选参数时，
 * 根据 ToolCallSchema 中定义的默认值自动填充。
 */
@Component
public class ParameterFiller {

    private static final Logger log = LoggerFactory.getLogger(ParameterFiller.class);

    /**
     * 对大模型返回的 JSON 执行参数补齐
     *
     * @param json 校验通过的 JSON 对象
     * @return 补齐后的 JSON（新增字段用 set 写入）
     */
    public JSONObject fillDefaults(JSONObject json) {
        String toolName = json.getStr(ToolCallSchema.FIELD_TOOL_NAME);

        if ("none".equals(toolName)) {
            return json; // no-op 不需要参数
        }

        Map<String, List<ToolCallSchema.ParamDef>> defs = ToolCallSchema.getToolParamDefs();
        List<ToolCallSchema.ParamDef> paramDefs = defs.get(toolName);

        if (paramDefs == null || paramDefs.isEmpty()) {
            return json;
        }

        // 确保 parameters 对象存在
        JSONObject params = json.getJSONObject(ToolCallSchema.FIELD_PARAMETERS);
        if (params == null) {
            params = new JSONObject();
            json.set(ToolCallSchema.FIELD_PARAMETERS, params);
        }

        int filledCount = 0;
        for (ToolCallSchema.ParamDef def : paramDefs) {
            if (!params.containsKey(def.name()) && def.defaultValue() != null) {
                params.set(def.name(), def.defaultValue());
                filledCount++;
                log.info("参数补齐: toolName={}, param={}, defaultValue={}", toolName, def.name(), def.defaultValue());
            }
        }

        if (filledCount > 0) {
            log.info("参数补齐完成: toolName={}, 补齐 {} 个参数", toolName, filledCount);
        }

        return json;
    }
}