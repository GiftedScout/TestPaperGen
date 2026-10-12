package com.teemo.testpapergeneration.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.entity.QuestionBank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * OpenAI 兼容协议的大模型客户端。
 * <p>
 * 请求：POST {baseUrl}/chat/completions，Authorization: Bearer {apiKey}
 * 响应：解析 choices[0].message.content
 * 超时：连接与读取均使用 {@link AiConfig#getTimeoutMs()}（需求约束 15s）
 */
@Component
public class OpenAiCompatibleClient implements AiClient {

    private final AiConfig aiConfig;

    private final RestTemplate restTemplate;

    @Autowired
    public OpenAiCompatibleClient(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) aiConfig.getTimeoutMs());
        factory.setReadTimeout((int) aiConfig.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generateAnalysis(QuestionBank question) throws AiException {
        if (aiConfig.getApiKey() == null || aiConfig.getApiKey().trim().isEmpty()) {
            throw new AiException("AI 接口未配置 apiKey，请在 application.yml 或环境变量 AI_API_KEY 中配置");
        }
        // 1、构造请求体
        JSONObject body = new JSONObject();
        body.put("model", aiConfig.getModel());
        body.put("temperature", aiConfig.getTemperature());
        body.put("max_tokens", aiConfig.getMaxTokens());
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一名严谨的高校教师，擅长编写简明准确的试题解析。");
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", buildPrompt(question));
        messages.add(systemMessage);
        messages.add(userMessage);
        body.put("messages", messages);

        // 2、发送请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());
        HttpEntity<String> entity = new HttpEntity<String>(body.toJSONString(), headers);
        String responseBody;
        try {
            responseBody = restTemplate.postForObject(
                    aiConfig.getBaseUrl() + "/chat/completions", entity, String.class);
        } catch (HttpStatusCodeException e) {
            throw new AiException("大模型接口返回错误：HTTP " + e.getStatusCode()
                    + "，" + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new AiException("大模型接口调用失败（网络异常或超时）：" + e.getMessage(), e);
        }

        // 3、解析响应，格式不稳定时抛出 AiException，由上层给出友好提示
        try {
            JSONObject json = JSONObject.parseObject(responseBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AiException("大模型返回格式异常：缺少 choices 字段");
            }
            String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
            if (content == null || content.trim().isEmpty()) {
                throw new AiException("大模型返回内容为空");
            }
            return content.trim();
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException("大模型返回格式解析失败：" + e.getMessage(), e);
        }
    }

    /** 按配置模板填充提示词 */
    private String buildPrompt(QuestionBank question) {
        String prompt = aiConfig.getPromptTemplate();
        prompt = prompt.replace("{topicType}", nullToEmpty(question.getTopic_type()));
        prompt = prompt.replace("{topic}", nullToEmpty(question.getTopic()));
        prompt = prompt.replace("{answer}", nullToEmpty(question.getAnswer()));
        return prompt;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
