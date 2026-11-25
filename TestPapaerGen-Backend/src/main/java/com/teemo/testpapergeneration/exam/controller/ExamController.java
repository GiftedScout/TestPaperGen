package com.teemo.testpapergeneration.exam.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.exam.dto.StudentPaperView;
import com.teemo.testpapergeneration.exam.dto.StudentResultView;
import com.teemo.testpapergeneration.exam.service.ExamException;
import com.teemo.testpapergeneration.exam.service.ExamService;
import com.teemo.testpapergeneration.utils.MyJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务包 A 接口：发布、作答、提交、结果与报告。
 * 统一返回 {code, msg, data} 且 Content-Type 为 application/json。
 */
@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
public class ExamController {

    private static final Logger log = LoggerFactory.getLogger(ExamController.class);
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final ExamService examService;
    private final MyJsonResponse response = new MyJsonResponse();

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    // ------------------------------------------------------------------
    // 教师端
    // ------------------------------------------------------------------

    @PostMapping("/exam/publish")
    public String publish(@RequestBody(required = false) String body, HttpSession session) {
        requireTeacher(session);
        JSONObject payload = parseBody(body);
        String paperUid = payload.getString("testPaperUid");
        String scope = payload.getString("scope");
        List<String> students = null;
        JSONArray array = payload.getJSONArray("studentUsernames");
        if (array != null) {
            students = array.toJavaList(String.class);
        }
        String deadlineText = payload.getString("deadline");
        if (deadlineText == null || deadlineText.trim().isEmpty()) {
            throw ExamException.badRequest("请设置截止时间");
        }
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
        format.setLenient(false);
        java.util.Date deadline;
        try {
            deadline = format.parse(deadlineText.trim());
        } catch (Exception e) {
            throw ExamException.badRequest("截止时间格式应为 " + DATE_PATTERN);
        }
        return response.make200Resp("发布成功", examService.publish(username(session), paperUid, scope, students, deadline));
    }

    @GetMapping("/exam/publish/list")
    public String publishes(HttpSession session) {
        requireTeacher(session);
        return response.make200Resp(MyJsonResponse.default_200_response,
                examService.listPublishes(username(session), isAdmin(session)));
    }

    @GetMapping("/exam/publish/{id}/submissions")
    public String submissions(@PathVariable("id") Integer id, HttpSession session) {
        requireTeacher(session);
        return response.make200Resp(MyJsonResponse.default_200_response,
                examService.listSubmissions(id, username(session), isAdmin(session)));
    }

    @GetMapping("/exam/publish/{id}/report")
    public String classReport(@PathVariable("id") Integer id, HttpSession session) {
        requireTeacher(session);
        return response.make200Resp(MyJsonResponse.default_200_response,
                examService.classReport(id, username(session), isAdmin(session)));
    }

    @GetMapping("/exam/students")
    public String students(HttpSession session) {
        requireTeacher(session);
        return response.make200Resp(MyJsonResponse.default_200_response, examService.eligibleStudents());
    }

    @GetMapping("/exam/me")
    public String me(HttpSession session) {
        requireLogin(session);
        JSONObject data = new JSONObject();
        data.put("username", username(session));
        String role = role(session);
        data.put("role", role);
        data.put("teacher", "teacher".equals(role) || "admin".equals(role));
        return response.make200Resp(MyJsonResponse.default_200_response, data);
    }

    // ------------------------------------------------------------------
    // 学生端
    // ------------------------------------------------------------------

    @GetMapping("/exam/student/papers")
    public String myPapers(HttpSession session) {
        requireLogin(session);
        return response.make200Resp(MyJsonResponse.default_200_response,
                examService.studentPapers(username(session)));
    }

    @GetMapping("/exam/student/papers/{id}")
    public String paper(@PathVariable("id") Integer id, HttpSession session) {
        requireLogin(session);
        StudentPaperView view = examService.paperForAnswering(id, username(session));
        return response.make200Resp(MyJsonResponse.default_200_response, view);
    }

    @PostMapping("/exam/student/papers/{id}/submit")
    public String submit(@PathVariable("id") Integer id,
                         @RequestBody(required = false) String body,
                         HttpSession session) {
        requireLogin(session);
        JSONObject payload = parseBody(body);
        JSONArray answers = payload.getJSONArray("answers");
        if (answers == null) {
            throw ExamException.badRequest("缺少作答内容");
        }
        Map<Integer, String> answerMap = new LinkedHashMap<>();
        for (int i = 0; i < answers.size(); i++) {
            JSONObject item = answers.getJSONObject(i);
            if (item == null) {
                continue;
            }
            Integer questionId = item.getInteger("questionHistoryId");
            if (questionId == null) {
                throw ExamException.badRequest("作答缺少题目标识");
            }
            if (answerMap.containsKey(questionId)) {
                throw ExamException.badRequest("同一道题只能提交一份作答");
            }
            answerMap.put(questionId, answerText(item.get("answer")));
        }
        return response.make200Resp("提交成功", examService.submit(id, username(session), answerMap));
    }

    @GetMapping("/exam/student/papers/{id}/result")
    public String result(@PathVariable("id") Integer id, HttpSession session) {
        requireLogin(session);
        StudentResultView view = examService.result(id, username(session));
        return response.make200Resp(MyJsonResponse.default_200_response, view);
    }

    @GetMapping("/exam/report/student")
    public String studentReport(@RequestParam(value = "publishId", required = false) Integer publishId,
                                HttpSession session) {
        requireLogin(session);
        return response.make200Resp(MyJsonResponse.default_200_response,
                examService.studentReport(username(session), publishId));
    }

    // ------------------------------------------------------------------
    // 异常与工具
    // ------------------------------------------------------------------

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
        log.error("考试接口异常", e);
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

    /**
     * 兼容字符串与数组两种作答：数组按顺序用 | 拼接，保留空位（空位必失分）。
     */
    private String answerText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) {
                    builder.append('|');
                }
                Object item = array.get(i);
                builder.append(item == null ? "" : String.valueOf(item));
            }
            return builder.toString();
        }
        return String.valueOf(value);
    }

    private void requireLogin(HttpSession session) {
        if (username(session) == null) {
            throw ExamException.forbidden("请先登录");
        }
    }

    private void requireTeacher(HttpSession session) {
        requireLogin(session);
        String role = role(session);
        if (!"teacher".equals(role) && !"admin".equals(role)) {
            throw ExamException.forbidden("仅教师与管理员可操作");
        }
    }

    private String username(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("username");
        return value == null ? null : String.valueOf(value);
    }

    private String role(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("user_role");
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isAdmin(HttpSession session) {
        return "admin".equals(role(session));
    }
}
