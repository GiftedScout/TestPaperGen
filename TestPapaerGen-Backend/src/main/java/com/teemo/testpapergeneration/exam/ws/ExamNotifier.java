package com.teemo.testpapergeneration.exam.ws;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/**
 * 实时事件推送：发布试卷、收到提交、成绩就绪。
 */
@Component
public class ExamNotifier {

    private static final Logger log = LoggerFactory.getLogger(ExamNotifier.class);

    private final ExamWebSocketHandler handler;

    public ExamNotifier(ExamWebSocketHandler handler) {
        this.handler = handler;
    }

    public void notifyUser(String username, String type, JSONObject data) {
        if (username == null) {
            return;
        }
        JSONObject message = new JSONObject();
        message.put("type", type);
        message.put("data", data);
        String payload = message.toJSONString();
        Set<WebSocketSession> sessions = handler.sessionsOf(username);
        for (WebSocketSession session : sessions) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }
                }
            } catch (Exception e) {
                log.warn("实时通知发送失败：user={}, type={}, error={}", username, type, e.getMessage());
            }
        }
    }
}
