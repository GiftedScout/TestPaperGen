package com.teemo.testpapergeneration.exam.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 局域网实时通知端点：/ws/exam。
 * 握手复用 HttpSession（浏览器同源请求自动携带 Cookie）。
 */
@Configuration
@EnableWebSocket
public class ExamWebSocketConfig implements WebSocketConfigurer {

    private final ExamWebSocketHandler handler;

    public ExamWebSocketConfig(ExamWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/exam")
                .addInterceptors(new ExamHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
