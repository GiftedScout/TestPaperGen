package com.teemo.testpapergeneration.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.mapper.QuestionBankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiConfigControllerTest {

    private MockMvc mvc;
    private AiConfig config;

    @BeforeEach
    void setup() {
        config = new AiConfig();
        AiAnalysisService service = new AiAnalysisService(mock(QuestionBankMapper.class), config,
                mock(OpenAiCompatibleClient.class));
        AiAnalysisController controller = new AiAnalysisController(service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private JSONObject body(String text) throws Exception {
        return JSON.parseObject(text);
    }

    @Test
    void statusNeverReturnsPlainApiKey() throws Exception {
        config.setApiKey("sk-secret-value-1234");
        String text = mvc.perform(get("/aiAnalysis/status"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JSONObject data = body(text).getJSONObject("data");
        assertTrue(data.getBooleanValue("apiKeyConfigured"));
        assertEquals("sk-****1234", data.getString("apiKeyMasked"));
        assertFalse(text.contains("sk-secret-value-1234"));
    }

    @Test
    void adminCanSaveRuntimeConfig() throws Exception {
        String payload = "{\"enabled\":true,\"apiKey\":\"sk-live-key-9876\","
                + "\"baseUrl\":\"https://api.deepseek.com/v1\",\"model\":\"deepseek-chat\"}";
        String text = mvc.perform(post("/aiAnalysis/config").sessionAttr("user_role", "admin")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JSONObject resp = body(text);
        assertEquals(200, resp.getIntValue("code"));
        assertTrue(config.isEnabled());
        assertEquals("sk-live-key-9876", config.getApiKey());
        assertEquals("https://api.deepseek.com/v1", config.getBaseUrl());
        assertEquals("deepseek-chat", config.getModel());
        // response is masked
        assertFalse(text.contains("sk-live-key-9876"));
        assertEquals("sk-****9876", resp.getJSONObject("data").getString("apiKeyMasked"));
    }

    @Test
    void teacherCannotSaveRuntimeConfig() throws Exception {
        mvc.perform(post("/aiAnalysis/config").sessionAttr("user_role", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"apiKey\":\"sk-x\"}"))
                .andExpect(status().isOk());
        assertFalse(config.isEnabled());
        assertEquals("", config.getApiKey());
    }

    @Test
    void enablingWithoutApiKeyIsRejected() throws Exception {
        mvc.perform(post("/aiAnalysis/config").sessionAttr("user_role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk());
        assertFalse(config.isEnabled());
    }

    @Test
    void invalidBaseUrlIsRejected() throws Exception {
        String text = mvc.perform(post("/aiAnalysis/config").sessionAttr("user_role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseUrl\":\"ftp://bad\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(500, body(text).getIntValue("code"));
        assertEquals("https://api.openai.com/v1", config.getBaseUrl());
    }

    @Test
    void blankFieldKeepsPreviousValue() throws Exception {
        config.setApiKey("sk-existing-0000");
        mvc.perform(post("/aiAnalysis/config").sessionAttr("user_role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"apiKey\":\"\",\"baseUrl\":\"\",\"model\":\"\"}"))
                .andExpect(status().isOk());
        assertTrue(config.isEnabled());
        assertEquals("sk-existing-0000", config.getApiKey());
    }
}
