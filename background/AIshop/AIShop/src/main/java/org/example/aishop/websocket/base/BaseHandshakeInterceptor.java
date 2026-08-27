package org.example.aishop.websocket.base;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器抽象基类
 * <p>
 * 提供公共的 URL 参数解析方法（parseParam / parseStringParam），
 * 子类只需实现 extractAttributes() 定义需要提取哪些参数。
 */
public abstract class BaseHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 从 URL query 中提取参数并放入 attributes
     *
     * @param query      URL 查询字符串
     * @param attributes WebSocket session 属性容器
     * @return true 允许握手，false 拒绝
     */
    protected abstract boolean extractAttributes(String query, Map<String, Object> attributes);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query == null) return false;
        return extractAttributes(query, attributes);
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    // ==================== 公共解析方法 ====================

    protected Long parseParam(String query, String key) {
        if (query.contains(key + "=")) {
            try {
                String[] parts = query.split(key + "=");
                if (parts.length > 1) {
                    String val = parts[1].split("&")[0];
                    return Long.parseLong(val);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    protected String parseStringParam(String query, String key) {
        if (query.contains(key + "=")) {
            String[] parts = query.split(key + "=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        }
        return null;
    }
}