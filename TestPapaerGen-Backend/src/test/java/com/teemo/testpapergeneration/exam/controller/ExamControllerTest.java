package com.teemo.testpapergeneration.exam.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.entity.QuestionGenHistory;
import com.teemo.testpapergeneration.exam.entity.ExamAnswer;
import com.teemo.testpapergeneration.exam.entity.ExamPublish;
import com.teemo.testpapergeneration.exam.entity.ExamSubmission;
import com.teemo.testpapergeneration.exam.grading.AnswerJudge;
import com.teemo.testpapergeneration.exam.mapper.ExamAnswerMapper;
import com.teemo.testpapergeneration.exam.mapper.ExamPaperMapper;
import com.teemo.testpapergeneration.exam.mapper.ExamPublishMapper;
import com.teemo.testpapergeneration.exam.mapper.ExamSubmissionMapper;
import com.teemo.testpapergeneration.exam.mapper.ExamUserMapper;
import com.teemo.testpapergeneration.exam.service.ExamService;
import com.teemo.testpapergeneration.exam.ws.ExamNotifier;
import com.teemo.testpapergeneration.mapper.QuestionBankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExamControllerTest {

    private MockMvc mvc;
    private ExamPublishMapper publishMapper;
    private ExamSubmissionMapper submissionMapper;
    private ExamAnswerMapper answerMapper;
    private ExamPaperMapper paperMapper;
    private ExamUserMapper userMapper;
    private QuestionBankMapper questionBankMapper;
    private ExamNotifier notifier;

    private ExamPublish publish;
    private ExamSubmission submission;

    @BeforeEach
    void setup() {
        publishMapper = mock(ExamPublishMapper.class);
        submissionMapper = mock(ExamSubmissionMapper.class);
        answerMapper = mock(ExamAnswerMapper.class);
        paperMapper = mock(ExamPaperMapper.class);
        userMapper = mock(ExamUserMapper.class);
        questionBankMapper = mock(QuestionBankMapper.class);
        notifier = mock(ExamNotifier.class);

        publish = new ExamPublish();
        publish.setId(1);
        publish.setTest_paper_uid("paper-1");
        publish.setTest_paper_name("期末测试");
        publish.setPublisher("teacher001");
        publish.setTarget_scope("all");
        publish.setDeadline(new Date(System.currentTimeMillis() + 3_600_000L));
        publish.setTotal_score(20.0);
        publish.setQuestion_count(3);
        publish.setCreate_time(new Date());
        when(publishMapper.getById(1)).thenReturn(publish);

        List<QuestionGenHistory> histories = Arrays.asList(
                history(11, 101, "选择题", 5.0, "第一章", "函数"),
                history(12, 102, "简答题", 10.0, "第二章", "证明"),
                history(13, 103, "选择题", 5.0, "第一章", "极限"));
        when(paperMapper.listHistoriesByPaperUid("paper-1")).thenReturn(histories);

        when(questionBankMapper.getQuestionBankById(101))
                .thenReturn(Collections.singletonList(bank(101, "A", 1, "解析1")));
        when(questionBankMapper.getQuestionBankById(102))
                .thenReturn(Collections.singletonList(bank(102, "参考答案", 1, "")));
        when(questionBankMapper.getQuestionBankById(103))
                .thenReturn(Collections.singletonList(bank(103, "B", 0, "")));

        submission = new ExamSubmission();
        submission.setId(21);
        submission.setPublish_id(1);
        submission.setStudent_username("stu001");
        submission.setStatus(ExamSubmission.STATUS_ASSIGNED);
        submission.setTotal_score(20.0);
        submission.setScore(0.0);
        submission.setPending_count(0);
        submission.setUngraded_count(0);
        when(submissionMapper.get(1, "stu001")).thenReturn(submission);
        when(submissionMapper.markSubmitted(any(ExamSubmission.class))).thenReturn(1);

        ExamService service = new ExamService(publishMapper, submissionMapper, answerMapper,
                paperMapper, userMapper, questionBankMapper, new AnswerJudge(), notifier);
        mvc = MockMvcBuilders.standaloneSetup(new ExamController(service)).build();
    }

    @Test
    void studentCannotPublish() throws Exception {
        JSONObject body = new JSONObject();
        body.put("testPaperUid", "paper-1");
        body.put("scope", "all");
        body.put("deadline", futureDeadline());

        JSONObject result = json(asStudent(post("/exam/publish"))
                .contentType(MediaType.APPLICATION_JSON).content(body.toJSONString()));

        assertEquals(403, result.getIntValue("code"));
        verify(publishMapper, never()).insert(any(ExamPublish.class));
    }

    @Test
    void teacherPublishesToAllStudents() throws Exception {
        when(userMapper.listStudentUsernames()).thenReturn(Arrays.asList("stu001", "stu002"));
        doAnswer(invocation -> {
            ((ExamPublish) invocation.getArgument(0)).setId(9);
            return 1;
        }).when(publishMapper).insert(any(ExamPublish.class));

        JSONObject body = new JSONObject();
        body.put("testPaperUid", "paper-1");
        body.put("scope", "all");
        body.put("deadline", futureDeadline());

        JSONObject result = json(asTeacher(post("/exam/publish"))
                .contentType(MediaType.APPLICATION_JSON).content(body.toJSONString()));

        assertEquals(200, result.getIntValue("code"));
        assertEquals(9, result.getJSONObject("data").getIntValue("publishId"));
        assertEquals(2, result.getJSONObject("data").getIntValue("targetCount"));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<ExamSubmission>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(submissionMapper).batchInsert(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(9, captor.getValue().get(0).getPublish_id().intValue());
        assertEquals(ExamSubmission.STATUS_ASSIGNED, captor.getValue().get(0).getStatus());
        verify(notifier).notifyUser(eq("stu001"), eq("paper.published"), any(JSONObject.class));
        verify(notifier).notifyUser(eq("stu002"), eq("paper.published"), any(JSONObject.class));
    }

    @Test
    void teacherOnlyApisRejectStudents() throws Exception {
        assertEquals(403, json(asStudent(get("/exam/publish/list"))).getIntValue("code"));
        assertEquals(403, json(asStudent(get("/exam/publish/1/report"))).getIntValue("code"));
        assertEquals(403, json(asStudent(get("/exam/students"))).getIntValue("code"));
        assertEquals(403, json(asStudent(get("/exam/publish/1/submissions"))).getIntValue("code"));
    }

    @Test
    void studentCannotOpenPaperAssignedToAnotherStudent() throws Exception {
        when(submissionMapper.get(1, "stu002")).thenReturn(null);
        JSONObject result = json(get("/exam/student/papers/1")
                .sessionAttr("username", "stu002").sessionAttr("user_role", "user"));

        assertEquals(403, result.getIntValue("code"));
    }

    @Test
    void answeringPayloadContainsNoAnswerOrAnalysis() throws Exception {
        JSONObject result = json(asStudent(get("/exam/student/papers/1")));

        assertEquals(200, result.getIntValue("code"));
        JSONArray questions = result.getJSONObject("data").getJSONArray("questions");
        assertEquals(3, questions.size());
        JSONObject first = questions.getJSONObject(0);
        assertEquals(11, first.getIntValue("questionHistoryId"));
        assertEquals(1, first.getIntValue("blankCount"));
        assertNull(first.get("answer"));
        assertNull(first.get("analysis"));
        assertNull(first.get("correctAnswer"));
        assertFalse(result.toJSONString().contains("解析1"));
        assertFalse(result.toJSONString().contains("参考答案"));
    }

    @Test
    void expiredPaperRejectsSubmitBeforeAnyWrite() throws Exception {
        publish.setDeadline(new Date(System.currentTimeMillis() - 60_000L));

        JSONObject result = json(asStudent(post("/exam/student/papers/1/submit"))
                .contentType(MediaType.APPLICATION_JSON).content(submitBody()));

        assertEquals(500, result.getIntValue("code"));
        assertTrue(result.getString("msg").contains("截止"));
        verify(answerMapper, never()).batchInsert(anyList());
        verify(submissionMapper, never()).markSubmitted(any(ExamSubmission.class));
    }

    @Test
    void duplicateSubmitRejected() throws Exception {
        submission.setStatus(ExamSubmission.STATUS_SUBMITTED);

        JSONObject result = json(asStudent(post("/exam/student/papers/1/submit"))
                .contentType(MediaType.APPLICATION_JSON).content(submitBody()));

        assertEquals(500, result.getIntValue("code"));
        assertTrue(result.getString("msg").contains("重复提交"));
        verify(answerMapper, never()).batchInsert(anyList());
        verify(submissionMapper, never()).markSubmitted(any(ExamSubmission.class));
    }

    @Test
    void submitRejectsQuestionOutsidePaper() throws Exception {
        String body = "{\"answers\":[{\"questionHistoryId\":999,\"answer\":\"A\"}]}";

        JSONObject result = json(asStudent(post("/exam/student/papers/1/submit"))
                .contentType(MediaType.APPLICATION_JSON).content(body));

        assertEquals(500, result.getIntValue("code"));
        assertTrue(result.getString("msg").contains("不属于本试卷"));
        verify(answerMapper, never()).batchInsert(anyList());
    }

    @Test
    void submitGradesConfirmedObjectiveAndMarksPendingAndUngraded() throws Exception {
        JSONObject result = json(asStudent(post("/exam/student/papers/1/submit"))
                .contentType(MediaType.APPLICATION_JSON).content(submitBody()));

        assertEquals(200, result.getIntValue("code"));
        JSONObject data = result.getJSONObject("data");
        assertEquals(5.0, data.getDoubleValue("score"), 0.001);
        assertEquals(5.0, data.getDoubleValue("objectivePossible"), 0.001);
        assertEquals(1, data.getIntValue("pendingCount"));
        assertEquals(1, data.getIntValue("ungradedCount"));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<ExamAnswer>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(answerMapper).batchInsert(captor.capture());
        List<ExamAnswer> rows = captor.getValue();
        assertEquals(3, rows.size());
        assertEquals(ExamAnswer.RESULT_CORRECT, rows.get(0).getJudge_result());
        assertEquals(5.0, rows.get(0).getScore_awarded(), 0.001);
        assertEquals(ExamAnswer.RESULT_PENDING, rows.get(1).getJudge_result());
        assertEquals(0.0, rows.get(1).getScore_awarded(), 0.001);
        assertEquals(ExamAnswer.RESULT_UNGRADED, rows.get(2).getJudge_result());
        assertEquals(0.0, rows.get(2).getScore_awarded(), 0.001);

        verify(submissionMapper).markSubmitted(submission);
        assertEquals(5.0, submission.getScore(), 0.001);
        assertEquals(1, submission.getPending_count().intValue());
        assertEquals(1, submission.getUngraded_count().intValue());
        verify(notifier).notifyUser(eq("teacher001"), eq("submission.received"), any(JSONObject.class));
        verify(notifier).notifyUser(eq("stu001"), eq("result.ready"), any(JSONObject.class));
    }

    @Test
    void resultRequiresSubmission() throws Exception {
        JSONObject result = json(asStudent(get("/exam/student/papers/1/result")));

        assertEquals(500, result.getIntValue("code"));
        assertTrue(result.getString("msg").contains("提交后"));
    }

    @Test
    void resultHidesUnconfirmedAnswerAndExplainsPending() throws Exception {
        submission.setStatus(ExamSubmission.STATUS_SUBMITTED);
        submission.setScore(5.0);
        submission.setSubmit_time(new Date());
        submission.setPending_count(1);
        submission.setUngraded_count(1);
        when(answerMapper.listBySubmission(21)).thenReturn(Arrays.asList(
                answerRow(11, "a", ExamAnswer.RESULT_CORRECT, 5.0),
                answerRow(12, "我的作答", ExamAnswer.RESULT_PENDING, 0.0),
                answerRow(13, "B", ExamAnswer.RESULT_UNGRADED, 0.0)));

        JSONObject result = json(asStudent(get("/exam/student/papers/1/result")));

        assertEquals(200, result.getIntValue("code"));
        JSONArray questions = result.getJSONObject("data").getJSONArray("questions");
        assertEquals("A", questions.getJSONObject(0).getString("correctAnswer"));
        assertEquals("解析1", questions.getJSONObject(0).getString("analysis"));
        assertEquals("A", questions.getJSONObject(0).getString("correctAnswer"));
        assertNull(questions.getJSONObject(2).getString("correctAnswer"));
        assertTrue(questions.getJSONObject(2).getString("analysis").contains("尚未经教师确认"));
        assertTrue(questions.getJSONObject(2).getString("reason").contains("不计分"));
        assertTrue(questions.getJSONObject(1).getString("reason").contains("待教师评分"));
    }

    @Test
    void classReportLimitedToOwnerTeacherAndAdmin() throws Exception {
        JSONObject forbidden = json(get("/exam/publish/1/report")
                .sessionAttr("username", "teacher002").sessionAttr("user_role", "teacher"));
        assertEquals(403, forbidden.getIntValue("code"));

        when(submissionMapper.listByPublish(1)).thenReturn(Collections.singletonList(submission));
        JSONObject allowed = json(get("/exam/publish/1/report")
                .sessionAttr("username", "admin").sessionAttr("user_role", "admin"));
        assertEquals(200, allowed.getIntValue("code"));
    }

    @Test
    void studentReportAggregatesOnlyGradedObjectiveQuestions() throws Exception {
        submission.setStatus(ExamSubmission.STATUS_SUBMITTED);
        submission.setScore(5.0);
        submission.setSubmit_time(new Date());
        submission.setPending_count(1);
        submission.setUngraded_count(1);
        when(submissionMapper.listByStudent("stu001")).thenReturn(Collections.singletonList(submission));
        when(answerMapper.listBySubmission(21)).thenReturn(Arrays.asList(
                answerRow(11, "a", ExamAnswer.RESULT_CORRECT, 5.0),
                answerRow(12, "我的作答", ExamAnswer.RESULT_PENDING, 0.0),
                answerRow(13, "B", ExamAnswer.RESULT_UNGRADED, 0.0)));

        JSONObject result = json(asStudent(get("/exam/report/student")));

        assertEquals(200, result.getIntValue("code"));
        JSONObject data = result.getJSONObject("data");
        assertEquals(5.0, data.getDoubleValue("totalPossible"), 0.001);
        assertEquals(5.0, data.getDoubleValue("totalEarned"), 0.001);
        assertEquals(100.0, data.getDoubleValue("rate"), 0.001);
        assertEquals(1, data.getIntValue("pendingCount"));
        assertEquals(1, data.getIntValue("ungradedCount"));
        JSONArray chapters = data.getJSONArray("chapters");
        assertEquals(2, chapters.size());
        assertEquals("第二章", chapters.getJSONObject(0).getString("name"));
        assertEquals(0.0, chapters.getJSONObject(0).getDoubleValue("rate"), 0.001);
        assertEquals("第一章", chapters.getJSONObject(1).getString("name"));
        assertEquals(100.0, chapters.getJSONObject(1).getDoubleValue("rate"), 0.001);
        assertEquals(2, chapters.getJSONObject(1).getIntValue("questionCount"));
        assertEquals(1, chapters.getJSONObject(1).getIntValue("pendingCount"));
    }

    private MockHttpServletRequestBuilder asStudent(MockHttpServletRequestBuilder builder) {
        return builder.sessionAttr("username", "stu001").sessionAttr("user_role", "user");
    }

    private MockHttpServletRequestBuilder asTeacher(MockHttpServletRequestBuilder builder) {
        return builder.sessionAttr("username", "teacher001").sessionAttr("user_role", "teacher");
    }

    private JSONObject json(MockHttpServletRequestBuilder builder) throws Exception {
        String body = mvc.perform(builder).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JSONObject.parseObject(body);
    }

    private String futureDeadline() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(System.currentTimeMillis() + 7_200_000L));
    }

    private String submitBody() {
        return "{\"answers\":["
                + "{\"questionHistoryId\":11,\"answer\":\"a\"},"
                + "{\"questionHistoryId\":12,\"answer\":\"我的作答\"},"
                + "{\"questionHistoryId\":13,\"answer\":\"B\"}]}";
    }

    private QuestionGenHistory history(int id, int bankId, String type, double score,
                                       String chapter, String label) {
        QuestionGenHistory history = new QuestionGenHistory();
        history.setId(id);
        history.setQuestion_bank_id(bankId);
        history.setTopic_type(type);
        history.setScore(score);
        history.setTopic("题干" + id);
        history.setChapter_1(chapter);
        history.setLabel_1(label);
        return history;
    }

    private QuestionBank bank(int id, String answer, int confirmStatus, String analysis) {
        QuestionBank bank = new QuestionBank();
        bank.setId(id);
        bank.setAnswer(answer);
        bank.setAnswer_confirm_status(confirmStatus);
        bank.setAnalysis(analysis);
        return bank;
    }

    private ExamAnswer answerRow(int historyId, String raw, String judgeResult, double score) {
        ExamAnswer answer = new ExamAnswer();
        answer.setQuestion_history_id(historyId);
        answer.setAnswer_raw(raw);
        answer.setJudge_result(judgeResult);
        answer.setScore_awarded(score);
        return answer;
    }
}
