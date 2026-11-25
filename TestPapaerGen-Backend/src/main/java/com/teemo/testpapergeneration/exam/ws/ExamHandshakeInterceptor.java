package com.teemo.testpapergeneration.exam.ws;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * WebSocket 握手鉴权：复用既有 HttpSession，未登录直接拒绝。
 */
public class ExamHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        HttpSession session = ((ServletServerHttpRequest) request).getServletRequest().getSession(false);
        if (session == null) {
            return false;
        }
        Object username = session.getAttribute("username");
        if (username == null || String.valueOf(username).trim().isEmpty()) {
            return false;
        }
        attributes.put("username", String.valueOf(username));
        Object role = session.getAttribute("user_role");
        attributes.put("user_role", role == null ? "" : String.valueOf(role));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
