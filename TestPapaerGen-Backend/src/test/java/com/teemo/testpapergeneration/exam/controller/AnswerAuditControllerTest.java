package com.teemo.testpapergeneration.exam.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.ai.AiClient;
import com.teemo.testpapergeneration.ai.AiConfig;
import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.exam.entity.QuestionAnswerAudit;
import com.teemo.testpapergeneration.exam.grading.AnswerJudge;
import com.teemo.testpapergeneration.exam.mapper.ExamPaperMapper;
import com.teemo.testpapergeneration.exam.mapper.QuestionAnswerAuditMapper;
import com.teemo.testpapergeneration.exam.service.AnswerAuditService;
import com.teemo.testpapergeneration.mapper.QuestionBankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnswerAuditControllerTest {

    private MockMvc mvc;
    private ExamPaperMapper paperMapper;
    private QuestionBankMapper bankMapper;
    private QuestionAnswerAuditMapper auditMapper;
    private AiConfig aiConfig;
    private AiClient aiClient;

    private final Map<Integer, QuestionAnswerAudit> auditStore = new HashMap<>();
    private final AtomicInteger auditIds = new AtomicInteger();

    @BeforeEach
    void setup() {
        paperMapper = mock(ExamPaperMapper.class);
        bankMapper = mock(QuestionBankMapper.class);
        auditMapper = mock(QuestionAnswerAuditMapper.class);
        aiConfig = mock(AiConfig.class);
        aiClient = mock(AiClient.class);

        when(aiConfig.isEnabled()).thenReturn(false);
        when(auditMapper.latestAll()).thenReturn(Collections.emptyList());
        when(auditMapper.insert(any(QuestionAnswerAudit.class))).thenAnswer(invocation -> {
            QuestionAnswerAudit audit = invocation.getArgument(0);
            if (audit.getId() == null) {
                audit.setId(auditIds.incrementAndGet());
            }
            auditStore.put(audit.getId(), audit);
            return 1;
        });
        when(auditMapper.getById(anyInt())).thenAnswer(invocation -> auditStore.get(invocation.getArgument(0)));
        when(auditMapper.markConfirmed(anyInt(), anyString(), anyString(), any(Date.class))).thenReturn(1);
        when(paperMapper.updateAnswerSuggestion(anyInt(), anyString(), anyDouble(), anyString())).thenReturn(1);
        when(paperMapper.confirmAnswer(anyInt(), anyString(), anyString(), any(Date.class))).thenReturn(1);

        AnswerAuditService service = new AnswerAuditService(paperMapper, bankMapper, auditMapper,
                new AnswerJudge(), aiConfig, aiClient);
        mvc = MockMvcBuilders.standaloneSetup(new AnswerAuditController(service)).build();
    }

    @Test
    void answerGovernanceIsTeacherOnly() throws Exception {
        JSONObject student = json(get("/exam/answers/pending")
                .sessionAttr("username", "stu001").sessionAttr("user_role", "user"));
        assertEquals(403, student.getIntValue("code"));

        JSONObject anonymous = json(get("/exam/answers/pending"));
        assertEquals(403, anonymous.getIntValue("code"));
    }

    @Test
    void suggestingTenAndConfirmingFiveOnlyWritesFiveAnswers() throws Exception {
        for (int id = 1; id <= 10; id++) {
            QuestionBank question = question(id, "选择题", "B", 0, null);
            when(bankMapper.getQuestionBankById(id)).thenReturn(Collections.singletonList(question));
        }
        List<Integer> ids = new ArrayList<>();
        for (int id = 1; id <= 10; id++) {
            ids.add(id);
        }
        JSONObject suggestBody = new JSONObject();
        suggestBody.put("questionIds", ids);

        JSONObject suggested = json(asTeacher(post("/exam/answers/suggest"))
                .contentType(MediaType.APPLICATION_JSON).content(suggestBody.toJSONString()));

        assertEquals(200, suggested.getIntValue("code"));
        JSONArray data = suggested.getJSONArray("data");
        assertEquals(10, data.size());
        assertEquals("A", data.getJSONObject(0).getString("suggestedAnswer"));
        assertTrue(data.getJSONObject(0).getBooleanValue("lowConfidence"));
        verify(paperMapper, times(10)).updateAnswerSuggestion(anyInt(), eq("A"), eq(0.3), eq("mock"));
        verify(paperMapper, never()).confirmAnswer(anyInt(), anyString(), anyString(), any(Date.class));

        for (int auditId = 1; auditId <= 5; auditId++) {
            JSONObject confirmBody = new JSONObject();
            confirmBody.put("auditId", auditId);
            confirmBody.put("finalAnswer", "A");
            JSONObject confirmed = json(asTeacher(post("/exam/answers/confirm"))
                    .contentType(MediaType.APPLICATION_JSON).content(confirmBody.toJSONString()));
            assertEquals(200, confirmed.getIntValue("code"));
        }

        verify(paperMapper, times(5)).confirmAnswer(anyInt(), eq("A"), eq("teacher001"), any(Date.class));
        verify(paperMapper, never()).confirmAnswer(eq(6), anyString(), anyString(), any(Date.class));
        verify(paperMapper, never()).confirmAnswer(eq(10), anyString(), anyString(), any(Date.class));
    }

    @Test
    void pendingSupportsLowConfidenceFilter() throws Exception {
        QuestionBank low = question(1, "选择题", "B", 0, 0.3);
        QuestionBank high = question(2, "选择题", "B", 0, 0.95);
        when(paperMapper.listUnconfirmed(null, null)).thenReturn(Arrays.asList(low, high));

        JSONObject all = json(asTeacher(get("/exam/answers/pending")));
        assertEquals(2, all.getJSONArray("data").size());

        JSONObject filtered = json(asTeacher(get("/exam/answers/pending").param("lowOnly", "true")));
        JSONArray data = filtered.getJSONArray("data");
        assertEquals(1, data.size());
        assertEquals(1, data.getJSONObject(0).getIntValue("questionId"));
        assertTrue(data.getJSONObject(0).getBooleanValue("lowConfidence"));
    }

    @Test
    void invalidAiSuggestionIsRecordedAsFailedAndNeverOverwritesAnswer() throws Exception {
        when(aiConfig.isEnabled()).thenReturn(true);
        QuestionBank question = question(1, "选择题", "B", 0, null);
        when(bankMapper.getQuestionBankById(1)).thenReturn(Collections.singletonList(question));
        when(aiClient.suggestAnswer(question)).thenReturn("{\"answer\":\"123\",\"confidence\":0.99}");

        JSONObject body = new JSONObject();
        body.put("questionIds", Collections.singletonList(1));

        JSONObject result = json(asTeacher(post("/exam/answers/suggest"))
                .contentType(MediaType.APPLICATION_JSON).content(body.toJSONString()));

        assertEquals(200, result.getIntValue("code"));
        JSONObject item = result.getJSONArray("data").getJSONObject(0);
        assertEquals(QuestionAnswerAudit.STATUS_FAILED, item.getString("status"));
        assertTrue(item.getString("failReason").contains("格式非法"));
        verify(paperMapper, never()).updateAnswerSuggestion(anyInt(), anyString(), anyDouble(), anyString());
        verify(paperMapper, never()).confirmAnswer(anyInt(), anyString(), anyString(), any(Date.class));
    }

    @Test
    void samplingReportsPerTypeAccuracy() throws Exception {
        when(auditMapper.listConfirmed()).thenReturn(Arrays.asList(
                audit("选择题", "A", "A"),
                audit("选择题", "A", "B"),
                audit("判断题", "对", "√")));

        JSONObject result = json(asTeacher(get("/exam/answers/sampling")));

        assertEquals(200, result.getIntValue("code"));
        JSONArray types = result.getJSONObject("data").getJSONArray("types");
        assertEquals(3, types.size());

        JSONObject choice = types.getJSONObject(0);
        assertEquals("选择题", choice.getString("topicType"));
        assertEquals(2, choice.getIntValue("confirmedCount"));
        assertEquals(1, choice.getIntValue("unchangedCount"));
        assertEquals(50.0, choice.getDoubleValue("accuracy"), 0.001);
        assertFalse(choice.getBooleanValue("pass"));

        JSONObject judge = types.getJSONObject(1);
        assertEquals("判断题", judge.getString("topicType"));
        assertEquals(100.0, judge.getDoubleValue("accuracy"), 0.001);
        assertTrue(judge.getBooleanValue("pass"));

        JSONObject fill = types.getJSONObject(2);
        assertEquals(0, fill.getIntValue("confirmedCount"));
        assertNull(fill.get("accuracy"));
    }

    private MockHttpServletRequestBuilder asTeacher(MockHttpServletRequestBuilder builder) {
        return builder.sessionAttr("username", "teacher001").sessionAttr("user_role", "teacher");
    }

    private JSONObject json(MockHttpServletRequestBuilder builder) throws Exception {
        String body = mvc.perform(builder).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JSONObject.parseObject(body);
    }

    private QuestionBank question(int id, String type, String answer, int confirmStatus, Double confidence) {
        QuestionBank question = new QuestionBank();
        question.setId(id);
        question.setTopic("题目" + id);
        question.setTopic_type(type);
        question.setAnswer(answer);
        question.setAnswer_confirm_status(confirmStatus);
        question.setAnswer_confidence(confidence);
        return question;
    }

    private QuestionAnswerAudit audit(String topicType, String suggested, String confirmed) {
        QuestionAnswerAudit audit = new QuestionAnswerAudit();
        audit.setTopic_type(topicType);
        audit.setSuggested_answer(suggested);
        audit.setConfirmed_answer(confirmed);
        audit.setStatus(QuestionAnswerAudit.STATUS_CONFIRMED);
        return audit;
    }
}
