package org.example.aishop.websocket.interceptor;

import org.example.aishop.websocket.base.BaseHandshakeInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 商家客服 WebSocket 握手拦截器
 * <p>
 * 从 URL 参数中提取 uid、type、targetMerchantId。
 */
@Component
public class MerchantHandshakeInterceptor extends BaseHandshakeInterceptor {

    @Override
    protected boolean extractAttributes(String query, Map<String, Object> attributes) {
        try {
            // 解析 uid（必填）
            Long uid = parseParam(query, "uid");
            if (uid == null) return false;
            attributes.put("uid", uid);

            // 解析 type: user=买家, merchant=商家
            String type = parseStringParam(query, "type");
            attributes.put("type", type != null ? type : "user");

            // 解析 targetMerchantId（买家连接时携带，商家表ID）
            Long targetMerchantId = parseParam(query, "targetMerchantId");
            if (targetMerchantId != null) {
                attributes.put("targetMerchantId", targetMerchantId);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}