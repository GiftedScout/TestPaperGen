package com.teemo.testpapergeneration.exam.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.exam.service.AnswerAuditService;
import com.teemo.testpapergeneration.exam.service.ExamException;
import com.teemo.testpapergeneration.utils.MyJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * 答案治理接口：仅教师与管理员可访问。
 * 人工确认是标准答案生效的唯一入口。
 */
@RestController
@RequestMapping(value = "/exam/answers", produces = "application/json;charset=UTF-8")
public class AnswerAuditController {

    private static final Logger log = LoggerFactory.getLogger(AnswerAuditController.class);

    private final AnswerAuditService auditService;
    private final MyJsonResponse response = new MyJsonResponse();

    public AnswerAuditController(AnswerAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/pending")
    public String pending(@RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "topicType", required = false) String topicType,
                          @RequestParam(value = "lowOnly", required = false, defaultValue = "false") boolean lowOnly,
                          HttpSession session) {
        requireTeacher(session);
        return response.make200Resp(MyJsonResponse.default_200_response,
                auditService.pending(keyword, topicType, lowOnly));
    }

    @PostMapping("/suggest")
    public String suggest(@RequestBody(required = false) String body, HttpSession session) {
        requireTeacher(session);
        JSONObject payload = parseBody(body);
        JSONArray array = payload.getJSONArray("questionIds");
        if (array == null || array.isEmpty()) {
            throw ExamException.badRequest("请选择要反推的题目");
        }
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            Integer id = array.getInteger(i);
            if (id != null) {
                ids.add(id);
            }
        }
        return response.make200Resp("反推完成", auditService.suggest(ids, username(session)));
    }

    @PostMapping("/confirm")
    public String confirm(@RequestBody(required = false) String body, HttpSession session) {
        requireTeacher(session);
        JSONObject payload = parseBody(body);
        Integer auditId = payload.getInteger("auditId");
        if (auditId == null) {
            throw ExamException.badRequest("缺少审计记录 id");
        }
        return response.make200Resp("确认成功",
                auditService.confirm(auditId, payload.getString("finalAnswer"), username(session)));
    }

    @PostMapping("/reject")
    public String reject(@RequestBody(required = false) String body, HttpSession session) {
        requireTeacher(session);
        JSONObject payload = parseBody(body);
        Integer auditId = payload.getInteger("auditId");
        if (auditId == null) {
            throw ExamException.badRequest("缺少审计记录 id");
        }
        return response.make200Resp("已驳回",
                auditService.reject(auditId, payload.getString("reason"), username(session)));
    }

    @GetMapping("/audits/{questionId}")
    public String audits(@PathVariable("questionId") Integer questionId, HttpSession session) {
        requireTeacher(session);
        return response.make200Resp(MyJsonResponse.default_200_response, auditService.audits(questionId));
    }

    @GetMapping("/sampling")
    public String sampling(HttpSession session) {
        requireTeacher(session);
        return response.make200Resp(MyJsonResponse.default_200_response, auditService.sampling());
    }

    @ExceptionHandler(ExamException.class)
    public ResponseEntity<String> handleExamException(ExamException e) {
        if (e.getCode() == 403) {
            return jsonResponse(response.make403Resp(e.getMessage()));
        }
        return jsonResponse(response.make500Resp(e.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return jsonResponse(response.make500Resp(e.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("答案治理接口异常", e);
        return jsonResponse(response.make500Resp("系统异常，请稍后重试", null));
    }

    private ResponseEntity<String> jsonResponse(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/json;charset=UTF-8"))
                .body(body);
    }

    private JSONObject parseBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw ExamException.badRequest("请求体不能为空");
        }
        try {
            JSONObject payload = JSONObject.parseObject(body);
            if (payload == null) {
                throw ExamException.badRequest("请求体格式错误");
            }
            return payload;
        } catch (ExamException e) {
            throw e;
        } catch (Exception e) {
            throw ExamException.badRequest("请求体不是合法 JSON");
        }
    }

    private void requireTeacher(HttpSession session) {
        Object username = session == null ? null : session.getAttribute("username");
        if (username == null) {
            throw ExamException.forbidden("请先登录");
        }
        Object role = session.getAttribute("user_role");
        String value = role == null ? "" : String.valueOf(role);
        if (!"teacher".equals(value) && !"admin".equals(value)) {
            throw ExamException.forbidden("仅教师与管理员可操作");
        }
    }

    private String username(HttpSession session) {
        Object value = session.getAttribute("username");
        return value == null ? null : String.valueOf(value);
    }
}
