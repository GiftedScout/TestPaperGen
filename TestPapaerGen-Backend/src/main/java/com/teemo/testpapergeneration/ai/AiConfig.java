package com.teemo.testpapergeneration.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 解析模块配置类。
 * <p>
 * 对应 application.yml 中 ai.* 配置段，详见 docs/03-AI解析模块.md。
 * 安全约束：apiKey 只允许出现在后端配置/环境变量中，严禁写入前端代码或提交到公开仓库。
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** 模块总开关。false 时使用 Mock 客户端返回占位解析，保证框架链路可演示、可联调 */
    private boolean enabled = false;

    /** 提供商标识，当前支持 openai-compatible（任何兼容 OpenAI chat/completions 协议的服务） */
    private String provider = "openai-compatible";

    /** 接口地址，例如 https://api.openai.com/v1 */
    private String baseUrl = "https://api.openai.com/v1";

    /** API Key，建议通过环境变量 AI_API_KEY 注入，不要明文提交 */
    private String apiKey = "";

    /** 模型名称 */
    private String model = "gpt-3.5-turbo";

    /** 请求超时时间（毫秒），需求约束 15s */
    private long timeoutMs = 15000;

    /** 单次生成最大 token 数 */
    private int maxTokens = 800;

    /** 采样温度，越低越稳定 */
    private double temperature = 0.3;

    /** 解析内容最大长度（字符），超过视为异常返回，防止大模型跑飞 */
    private int maxAnalysisLength = 2000;

    /**
     * 生成解析的提示词模板。
     * 占位符：{topicType} 题型、{topic} 题干、{answer} 答案。
     */
    private String promptTemplate =
            "你是一名经验丰富的高校教师，请为下面这道{topicType}生成试题解析。"
                    + "要求：1、聚焦解题思路与关键知识点，不要复述题目；"
                    + "2、与给定答案保持一致，不得出现矛盾结论；"
                    + "3、200字以内，纯文本，不要使用Markdown标记。"
                    + "【题目】{topic}"
                    + "【答案】{answer}";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxAnalysisLength() {
        return maxAnalysisLength;
    }

    public void setMaxAnalysisLength(int maxAnalysisLength) {
        this.maxAnalysisLength = maxAnalysisLength;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }
}
