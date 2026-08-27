package org.example.aishop.websocket.interceptor;

import org.example.aishop.websocket.base.BaseHandshakeInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 平台客服 WebSocket 握手拦截器
 * <p>
 * 从 URL 参数中提取 uid、type。
 */
@Component
public class PlatformCsHandshakeInterceptor extends BaseHandshakeInterceptor {

    @Override
    protected boolean extractAttributes(String query, Map<String, Object> attributes) {
        try {
            // 解析 uid（必填）
            Long uid = parseParam(query, "uid");
            if (uid == null) return false;
            attributes.put("uid", uid);

            // 解析 type: user=买家, admin=平台管理员
            String type = parseStringParam(query, "type");
            attributes.put("type", type != null ? type : "user");

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}