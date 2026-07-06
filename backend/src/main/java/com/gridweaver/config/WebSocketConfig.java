package com.gridweaver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.gridweaver.handler.IoTWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // Read from application.properties — no hardcoded values
    @Value("${gridweaver.cors.allowed-origin}")
    private String allowedOrigin;

    private final IoTWebSocketHandler ioTWebSocketHandler;

    public WebSocketConfig(IoTWebSocketHandler ioTWebSocketHandler) {
        this.ioTWebSocketHandler = ioTWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(ioTWebSocketHandler, "/ws/iot")
            .setAllowedOrigins(allowedOrigin);
    }
}