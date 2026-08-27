package org.example.aishop.ai.capability.mcp;

/**
 * 远程 MCP 工具配置 — 描述一个部署在独立微服务中的 MCP 工具
 *
 * 用于 McpToolRegistry 注册远程工具，支持 HTTP / MCP 协议两种对接方式。
 *
 * <pre>
 * 示例：
 *   new RemoteMcpToolConfig(
 *       "cart_query_remote",
 *       "http://cart-service:8082/mcp/cart/query",
 *       ProtocolType.HTTP_JSON
 *   )
 * </pre>
 */
public record RemoteMcpToolConfig(
        /** 工具唯一名称 */
        String toolName,
        /** 远程服务 URL */
        String url,
        /** 通信协议 */
        ProtocolType protocol
) {
    public enum ProtocolType {
        /** HTTP JSON 调用（POST body → JSON response） */
        HTTP_JSON,
        /** 标准 MCP 协议（JSON-RPC 2.0 over HTTP） */
        MCP_JSON_RPC
    }
}