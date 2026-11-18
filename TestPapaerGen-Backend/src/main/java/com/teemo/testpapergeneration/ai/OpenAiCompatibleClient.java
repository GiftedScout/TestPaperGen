package com.teemo.testpapergeneration.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.entity.QuestionBank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    /** 单次解析最多尝试次数：兼容中转网关偶发返回空内容 */
    private static final int MAX_ATTEMPTS = 3;

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
            throw new AiException("AI 接口未配置 apiKey，请在「AI 配置」页填写或通过环境变量 AI_API_KEY 注入");
        }
        AiException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return requestAnalysis(question);
            } catch (AiException e) {
                lastError = e;
                log.warn("AI 解析生成失败（第 {}/{} 次）：{}", attempt, MAX_ATTEMPTS, e.getMessage());
                // 鉴权/地址类错误重试无意义，直接返回
                if (!retryable(e.getMessage()) || attempt == MAX_ATTEMPTS) {
                    break;
                }
                sleepQuietly(500L * attempt);
            }
        }
        throw lastError;
    }

    /** 一次完整的模型调用：构造请求 -> 发送 -> 解析 choices[0].message.content */
    private String requestAnalysis(QuestionBank question) throws AiException {
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
            throw new AiException(friendly(e.getStatusCode().value(), e.getResponseBodyAsString()), e);
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
            JSONObject choice = choices.getJSONObject(0);
            String content = choice.getJSONObject("message") == null ? null
                    : choice.getJSONObject("message").getString("content");
            String finishReason = choice.getString("finish_reason");
            if (content == null || content.trim().isEmpty()) {
                throw new AiException("大模型返回内容为空（finish_reason=" + finishReason
                        + "），可能是网关限流或模型不支持该请求，请重试或更换模型。原始响应："
                        + brief(responseBody));
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

    /**
     * 连通性测试：发一次最小 chat 请求（max_tokens=1），验证地址、密钥、模型三者可用。
     * 与正式生成共用同一个 RestTemplate 超时约束（15s）。
     */
    public String ping(String baseUrl, String apiKey, String model) throws AiException {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 1);
        body.put("temperature", 0);
        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "ping");
        messages.add(userMessage);
        body.put("messages", messages);

        String responseBody = post(baseUrl, apiKey, "/chat/completions", body.toJSONString());
        try {
            JSONObject json = JSONObject.parseObject(responseBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "connected";
            }
            String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
            return content == null ? "connected" : content;
        } catch (Exception e) {
            return "connected";
        }
    }

    /**
     * 拉取模型列表（GET {baseUrl}/models），返回 [{id, ownedBy}]。
     */
    public JSONArray listModels(String baseUrl, String apiKey) throws AiException {
        String url = trimTrailingSlash(baseUrl) + "/models";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        String responseBody;
        try {
            responseBody = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<Void>(headers), String.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw new AiException(friendly(e.getStatusCode().value(), e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new AiException("无法连接模型服务（网络异常或超时）：" + e.getMessage(), e);
        }

        JSONArray result = new JSONArray();
        try {
            JSONObject json = JSONObject.parseObject(responseBody);
            JSONArray data = json.getJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    JSONObject model = new JSONObject();
                    model.put("id", item.getString("id"));
                    model.put("ownedBy", item.getString("owned_by"));
                    result.add(model);
                }
            }
        } catch (Exception e) {
            throw new AiException("模型列表解析失败：" + e.getMessage(), e);
        }
        return result;
    }

    private String post(String baseUrl, String apiKey, String path, String body) throws AiException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);
        try {
            return restTemplate.postForObject(trimTrailingSlash(baseUrl) + path, entity, String.class);
        } catch (HttpStatusCodeException e) {
            throw new AiException(friendly(e.getStatusCode().value(), e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new AiException("接口调用失败（网络异常或超时）：" + e.getMessage(), e);
        }
    }

    /** 把常见 HTTP 状态转成可执行的中文提示，原文附后便于排查 */
    private String friendly(int status, String body) {
        String hint;
        if (status == 401) {
            hint = "API Key 无效或未授权";
        } else if (status == 403) {
            hint = "无访问权限，请检查密钥权限或 IP 白名单";
        } else if (status == 404) {
            hint = "接口地址不存在，请确认 baseUrl 是否包含 /v1";
        } else if (status == 429) {
            hint = "请求过于频繁或额度不足";
        } else if (status >= 500) {
            hint = "模型服务端异常";
        } else {
            hint = "接口返回错误";
        }
        String detail = brief(body);
        return detail.isEmpty() ? hint + "（HTTP " + status + "）" : hint + "（HTTP " + status + "）：" + detail;
    }

    private String trimTrailingSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    /** 截断错误响应，避免前端出现超长堆栈 */
    private String brief(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 200 ? t.substring(0, 200) + "..." : t;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 鉴权失败、地址错误重试没有意义；空返回、限流、5xx、网络抖动值得重试 */
    private boolean retryable(String message) {
        if (message == null) {
            return false;
        }
        return !(message.contains("HTTP 401") || message.contains("HTTP 403")
                || message.contains("HTTP 404") || message.contains("未配置 apiKey"));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
