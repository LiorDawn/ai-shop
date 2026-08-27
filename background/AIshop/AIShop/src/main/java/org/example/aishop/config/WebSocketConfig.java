package org.example.aishop.config;

import org.example.aishop.websocket.handler.MerchantChatHandler;
import org.example.aishop.websocket.handler.PlatformChatHandler;
import org.example.aishop.websocket.interceptor.MerchantHandshakeInterceptor;
import org.example.aishop.websocket.interceptor.PlatformCsHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MerchantChatHandler merchantChatHandler;

    @Autowired
    private MerchantHandshakeInterceptor merchantHandshakeInterceptor;

    @Autowired
    private PlatformChatHandler platformChatHandler;

    @Autowired
    private PlatformCsHandshakeInterceptor platformCsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 商家客服 WebSocket（买家 ↔ 商家）
        registry.addHandler(merchantChatHandler, "/ws/merchant")
                .addInterceptors(merchantHandshakeInterceptor)
                .setAllowedOrigins("*");

        // 平台客服 WebSocket（买家 ↔ 平台管理员）
        registry.addHandler(platformChatHandler, "/ws/platform-cs")
                .addInterceptors(platformCsHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}