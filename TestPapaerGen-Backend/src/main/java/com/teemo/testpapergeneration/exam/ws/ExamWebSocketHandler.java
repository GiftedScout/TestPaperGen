package com.teemo.testpapergeneration.exam.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 按用户名维护 WebSocket 连接。对方不在线则消息丢弃（本期不引入离线队列）。
 */
@Component
public class ExamWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ExamWebSocketHandler.class);

    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = usernameOf(session);
        if (username == null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception ignore) {
                // no-op
            }
            return;
        }
        sessions.computeIfAbsent(username, key -> new CopyOnWriteArraySet<>()).add(session);
        log.info("考试实时通道已连接：{}", username);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = usernameOf(session);
        if (username == null) {
            return;
        }
        Set<WebSocketSession> userSessions = sessions.get(username);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(username);
            }
        }
        log.info("考试实时通道已断开：{}（{}）", username, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端无需上行消息；收到 ping 时回 pong，便于前端探活
        if (message != null && "ping".equalsIgnoreCase(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (Exception ignore) {
                // 连接可能已断开，忽略
            }
        }
    }

    public Set<WebSocketSession> sessionsOf(String username) {
        Set<WebSocketSession> userSessions = sessions.get(username);
        return userSessions == null ? Collections.emptySet() : userSessions;
    }

    private String usernameOf(WebSocketSession session) {
        Object username = session.getAttributes().get("username");
        return username == null ? null : String.valueOf(username);
    }
}
