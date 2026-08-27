package org.example.aishop.ai.capability.mcp;

import cn.hutool.json.JSONObject;
import org.example.aishop.dto.ai.ToolCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具注册中心（双适配：本地 Bean + 远程 HTTP/MCP 协议）
 *
 * 职责：
 * 1. 启动时自动扫描本地 @Component 工具
 * 2. 支持手动注册远程工具（HTTP JSON / MCP JSON-RPC）
 * 3. 根据 toolName 查找并执行（本地优先，远程兜底）
 * 4. 统一异常捕获 + 封装 ToolCallResult
 *
 * <h3>本地工具注册（自动）</h3>
 * 所有实现 McpToolDefinition 的 @Component Bean 在 @PostConstruct 时自动注册。
 *
 * <h3>远程工具注册（手动）</h3>
 * <pre>
 *  registry.registerRemote(new RemoteMcpToolConfig(
 *      "cart_query_remote",
 *      "http://cart-service:8082/mcp/cart/query",
 *      ProtocolType.HTTP_JSON
 *  ));
 * </pre>
 */
@Component
public class McpToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistry.class);

    /** 工具注册表：toolName → 工具定义（本地 Bean + 远程适配器） */
    private final Map<String, McpToolDefinition> registry = new ConcurrentHashMap<>();

    /** 远程工具配置列表（用于管理） */
    private final List<RemoteMcpToolConfig> remoteConfigs = new ArrayList<>();

    @Autowired(required = false)
    private List<McpToolDefinition> toolDefinitions;

    /**
     * 启动时自动注册所有本地 @Component 工具
     */
    @PostConstruct
    public void init() {
        if (toolDefinitions != null) {
            for (McpToolDefinition tool : toolDefinitions) {
                registry.put(tool.getToolName(), tool);
                log.info("MCP 本地工具注册: {} → {}", tool.getToolName(), tool.getClass().getSimpleName());
            }
        }
        log.info("MCP 工具注册中心初始化完成，本地工具: {} 个", registry.size());
    }

    // ================== 远程工具注册 ==================

    /**
     * 注册远程 MCP 工具（HTTP JSON / MCP JSON-RPC）
     *
     * 远程工具注册后与本地工具无差别调用，对上层 BaseAgent 完全透明。
     * 如果远程 toolName 与本地工具同名，远程会覆盖本地（远程优先）。
     *
     * @param config 远程工具配置
     */
    public void registerRemote(RemoteMcpToolConfig config) {
        McpToolDefinition adapter = new RemoteMcpToolAdapter(config);
        registry.put(config.toolName(), adapter);
        remoteConfigs.add(config);
        log.info("MCP 远程工具注册: {} → {} [{}]", config.toolName(), config.url(), config.protocol());
    }

    /**
     * 批量注册远程工具
     */
    public void registerRemoteBatch(List<RemoteMcpToolConfig> configs) {
        for (RemoteMcpToolConfig config : configs) {
            registerRemote(config);
        }
    }

    /**
     * 移除远程工具
     */
    public void unregisterRemote(String toolName) {
        if (registry.remove(toolName) != null) {
            remoteConfigs.removeIf(c -> c.toolName().equals(toolName));
            log.info("MCP 远程工具已移除: {}", toolName);
        }
    }

    // ================== 工具执行 ==================

    /**
     * 按名称查找并执行工具（本地 Bean 优先，远程适配器兜底）
     *
     * @param toolName 工具名称
     * @param toolJson 已校验+补齐参数的 JSON 对象
     * @return 统一工具执行结果
     */
    public ToolCallResult execute(String toolName, JSONObject toolJson) {
        long start = System.currentTimeMillis();

        McpToolDefinition tool = registry.get(toolName);
        if (tool == null) {
            log.error("未找到工具: {} (已注册工具: {})", toolName, registry.keySet());
            return ToolCallResult.failure(toolName, "工具 " + toolName + " 不存在",
                    "TOOL_NOT_FOUND", System.currentTimeMillis() - start);
        }

        try {
            JSONObject params = toolJson.getJSONObject("parameters");
            if (params == null) params = new JSONObject();

            String result = tool.execute(params);
            long elapsed = System.currentTimeMillis() - start;

            log.info("工具执行成功: toolName={}, elapsed={}ms, resultLen={}",
                    toolName, elapsed, result != null ? result.length() : 0);
            return ToolCallResult.success(toolName, result, null, elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("工具执行失败: toolName={}, error={}", toolName, errorMsg, e);

            return ToolCallResult.failure(toolName, errorMsg, e.getClass().getSimpleName(), elapsed);
        }
    }

    // ================== 查询方法 ==================

    /**
     * 获取所有已注册工具名称（本地 + 远程）
     */
    public List<String> getRegisteredToolNames() {
        return List.copyOf(registry.keySet());
    }

    /**
     * 获取已注册工具总数
     */
    public int getToolCount() {
        return registry.size();
    }

    /**
     * 判断工具是否为远程工具
     */
    public boolean isRemote(String toolName) {
        McpToolDefinition tool = registry.get(toolName);
        return tool instanceof RemoteMcpToolAdapter;
    }

    /**
     * 获取远程工具配置列表
     */
    public List<RemoteMcpToolConfig> getRemoteConfigs() {
        return List.copyOf(remoteConfigs);
    }
}