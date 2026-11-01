package com.teemo.testpapergeneration.controller;

import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.entity.QuestionGenHistory;
import com.teemo.testpapergeneration.mapper.*;
import com.teemo.testpapergeneration.services.GenWord;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalysisExportControllerTest {
    private MockMvc mvc;
    private GenWord genWord;
    private QuestionGenHistoryMapper histories;

    @BeforeEach
    void setup() {
        QuestionBankMapper bank = mock(QuestionBankMapper.class);
        histories = mock(QuestionGenHistoryMapper.class);
        TestPaperGenHistoryMapper papers = mock(TestPaperGenHistoryMapper.class);
        genWord = mock(GenWord.class);
        QuestionBank question = new QuestionBank();
        question.setId(1);
        question.setTopic("测试题干 < & >");
        question.setAnswer("测试答案");
        question.setScore(5.0);
        question.setDifficulty(2);
        question.setAnalysis("当前解析 < & >");
        when(bank.getQuestionBankById(1)).thenReturn(Collections.singletonList(question));
        QuestionGenHistory history = new QuestionGenHistory();
        history.setQuestion_bank_id(1);
        when(histories.getQuestionGenHistoriesByTestPaperUid("paper-1"))
                .thenReturn(Collections.singletonList(history));
        when(histories.insertQuestionGenHistories(anyList())).thenReturn(1);
        when(papers.insertTestPaperGenHistory(any())).thenReturn(1);
        QuestionGenController controller = new QuestionGenController(bank);
        ReflectionTestUtils.setField(controller, "genWord", genWord);
        ReflectionTestUtils.setField(controller, "questionGenHistoryMapper", histories);
        ReflectionTestUtils.setField(controller, "testPaperGenHistoryMapper", papers);
        mvc = MockMvcBuilders.standaloneSetup(controller,
                new QuestionGenHistoryController(histories, papers, bank)).build();
    }

    private String payload(String flag) {
        return "{\"questionIdList\":[1],\"TKTIdList\":[],\"XZTIdList\":[],\"PDTIdList\":[],"
                + "\"JDTIdList\":[],\"testPaperName\":\"测试\""
                + ("omitted".equals(flag) ? "" : ",\"withAnalysis\":" + flag) + "}";
    }

    private String exportText(MockHttpServletRequestBuilder request) throws Exception {
        byte[] bytes = mvc.perform(request).andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andReturn().getResponse().getContentAsByteArray();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return document.getParagraphs().stream().map(p -> p.getText()).collect(Collectors.joining("\n"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"omitted", "false", "true"})
    void newExportHonorsFlagAndStillSavesHistory(String flag) throws Exception {
        String text = exportText(post("/questionGen2").sessionAttr("username", "teacher")
                .contentType(MediaType.APPLICATION_JSON).content(payload(flag)));
        assertTrue(text.contains("测试题干 < & >"));
        assertEquals("true".equals(flag), text.contains("解析：当前解析 < & >"));
        verify(histories).insertQuestionGenHistories(anyList());
        verifyZeroInteractions(genWord);
    }

    @ParameterizedTest
    @ValueSource(strings = {"omitted", "false", "true"})
    void historyExportHonorsOptionalFlag(String flag) throws Exception {
        MockHttpServletRequestBuilder request = get("/reExportTestPaper").param("test_paper_uid", "paper-1");
        if (!"omitted".equals(flag)) request.param("withAnalysis", flag);
        String text = exportText(request);
        assertTrue(text.contains("测试题干 < & >"));
        assertEquals("true".equals(flag), text.contains("解析：当前解析 < & >"));
        verify(histories, never()).insertQuestionGenHistories(anyList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"omitted", "false", "true"})
    void legacyExportPassesFlagToGenWord(String flag) throws Exception {
        mvc.perform(post("/questionGen").sessionAttr("username", "teacher")
                .contentType(MediaType.APPLICATION_JSON).content(payload(flag))).andExpect(status().isOk());
        verify(genWord).genWordTest(anyList(), eq("测试"), eq("teacher"), eq("true".equals(flag)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void answerExportRemainsUnchanged(String flag) throws Exception {
        String text = exportText(get("/exportAnswer").param("test_paper_uid", "paper-1").param("withAnalysis", flag));
        assertTrue(text.contains("测试答案"));
        assertFalse(text.contains("解析："));
    }
}
