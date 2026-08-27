package org.example.aishop.ai.capability.mcp;

import cn.hutool.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 远程 MCP 工具适配器 — 将远程 HTTP/MCP 工具包装为 McpToolDefinition
 *
 * 当工具部署在独立微服务中时，通过此适配器进行 HTTP 调用，
 * 返回纯文本结果，对上层 McpToolRegistry 完全透明。
 */
public class RemoteMcpToolAdapter implements McpToolDefinition {

    private static final Logger log = LoggerFactory.getLogger(RemoteMcpToolAdapter.class);

    private final RemoteMcpToolConfig config;
    private final HttpClient httpClient;

    public RemoteMcpToolAdapter(RemoteMcpToolConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public String getToolName() {
        return config.toolName();
    }

    @Override
    public String execute(JSONObject params) throws Exception {
        return switch (config.protocol()) {
            case HTTP_JSON -> executeHttpJson(params);
            case MCP_JSON_RPC -> executeMcpJsonRpc(params);
        };
    }

    /**
     * HTTP JSON 调用：POST 参数 JSON → 返回纯文本 body
     */
    private String executeHttpJson(JSONObject params) throws Exception {
        String body = params.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.url()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        log.debug("远程工具调用 HTTP_JSON: toolName={}, url={}", config.toolName(), config.url());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("远程工具返回 HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    /**
     * MCP JSON-RPC 2.0 调用
     */
    private String executeMcpJsonRpc(JSONObject params) throws Exception {
        JSONObject rpcBody = new JSONObject();
        rpcBody.set("jsonrpc", "2.0");
        rpcBody.set("method", "tools/call");
        JSONObject rpcParams = new JSONObject();
        rpcParams.set("name", config.toolName());
        rpcParams.set("arguments", params);
        rpcBody.set("params", rpcParams);
        rpcBody.set("id", System.currentTimeMillis());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.url()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(rpcBody.toString()))
                .timeout(Duration.ofSeconds(10))
                .build();

        log.debug("远程工具调用 MCP_JSON_RPC: toolName={}, url={}", config.toolName(), config.url());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("MCP Server 返回 HTTP " + response.statusCode());
        }

        JSONObject rpcResponse = new JSONObject(response.body());
        if (rpcResponse.containsKey("error")) {
            throw new RuntimeException("MCP RPC 错误: " + rpcResponse.getJSONObject("error").getStr("message"));
        }

        Object result = rpcResponse.get("result");
        return result != null ? result.toString() : "";
    }
}