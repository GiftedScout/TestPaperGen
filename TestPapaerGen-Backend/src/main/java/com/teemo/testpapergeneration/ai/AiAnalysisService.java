package com.teemo.testpapergeneration.ai;

import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.mapper.QuestionBankMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AI 解析编排服务。
 * <p>
 * 职责：查题 -> 调用大模型 -> 返回内容校验 -> 落库 -> 返回最新题目。
 * 与原有组卷模块完全解耦：不修改遗传/贪心算法、不修改导出主流程，仅通过
 * QuestionBankMapper#updateAnalysisById 写入解析字段。
 * <p>
 * 容错策略（对应需求"对异常返回给出提示，不影响原有组卷主流程"）：
 * 任何一步失败均抛出 {@link AiException}，由 Controller 转为友好提示，不回写脏数据。
 */
@Service
public class AiAnalysisService {

    /** 解析状态：无解析 */
    public static final int STATUS_NONE = 0;
    /** 解析状态：已生成 */
    public static final int STATUS_DONE = 1;

    private final QuestionBankMapper questionBankMapper;

    private final AiConfig aiConfig;

    private final OpenAiCompatibleClient aiClient;

    @Autowired
    public AiAnalysisService(QuestionBankMapper questionBankMapper,
                             AiConfig aiConfig,
                             OpenAiCompatibleClient aiClient) {
        this.questionBankMapper = questionBankMapper;
        this.aiConfig = aiConfig;
        this.aiClient = aiClient;
    }

    /**
     * 生成并保存单题解析。
     *
     * @param questionId 题目 id
     * @return 保存解析后的最新题目
     * @throws AiException 题目不存在 / 调用失败 / 返回校验失败
     */
    public QuestionBank generateAndSave(Integer questionId) throws AiException {
        QuestionBank question = loadQuestion(questionId);
        String analysis = generate(question);
        saveAnalysis(questionId, analysis);
        return loadQuestion(questionId);
    }

    /**
     * 批量生成解析。顺序执行，单题失败不影响其余题目。
     *
     * @param questionIds 题目 id 列表
     * @return 逐题结果：[{questionId, success, analysis | error}]
     */
    public List<JSONObject> batchGenerate(List<Integer> questionIds) {
        List<JSONObject> results = new ArrayList<JSONObject>();
        for (Integer questionId : questionIds) {
            JSONObject item = new JSONObject();
            item.put("questionId", questionId);
            try {
                QuestionBank question = generateAndSave(questionId);
                item.put("success", true);
                item.put("analysis", question.getAnalysis());
            } catch (AiException e) {
                item.put("success", false);
                item.put("error", e.getMessage());
            } catch (Exception e) {
                item.put("success", false);
                item.put("error", "系统异常：" + e.getMessage());
            }
            results.add(item);
        }
        return results;
    }

    /**
     * 查询单题（含解析）。
     */
    public QuestionBank getAnalysis(Integer questionId) throws AiException {
        return loadQuestion(questionId);
    }

    /**
     * 手动修改/保存解析（教师不满意 AI 结果时可人工编辑）。
     */
    public Integer updateAnalysis(Integer questionId, String analysis) throws AiException {
        loadQuestion(questionId);
        if (analysis == null || analysis.trim().isEmpty()) {
            throw new AiException("解析内容不能为空");
        }
        if (analysis.length() > aiConfig.getMaxAnalysisLength()) {
            throw new AiException("解析内容超长（上限 "
                    + aiConfig.getMaxAnalysisLength() + " 字符）");
        }
        saveAnalysis(questionId, analysis.trim());
        return 1;
    }

    /** 当前模块状态（不返回 apiKey） */
    public JSONObject status() {
        JSONObject json = new JSONObject();
        json.put("enabled", aiConfig.isEnabled());
        json.put("provider", aiConfig.getProvider());
        json.put("model", aiConfig.getModel());
        json.put("timeoutMs", aiConfig.getTimeoutMs());
        return json;
    }

    // ---------- 内部方法 ----------

    /**
     * 调用大模型生成解析；ai.enabled=false 时走 Mock 分支返回占位内容，
     * 保证未配置密钥时框架链路依然可以完整演示。
     */
    private String generate(QuestionBank question) throws AiException {
        String raw;
        if (aiConfig.isEnabled()) {
            raw = aiClient.generateAnalysis(question);
        } else {
            raw = mockAnalysis(question);
        }
        return validate(raw);
    }

    /** 返回内容校验：非空、长度上限、去除首尾 Markdown 代码围栏 */
    private String validate(String raw) throws AiException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new AiException("解析生成失败：返回内容为空");
        }
        String content = raw.trim();
        if (content.startsWith("```")) {
            content = content.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "");
        }
        if (content.length() > aiConfig.getMaxAnalysisLength()) {
            throw new AiException("解析生成失败：返回内容超长（"
                    + content.length() + " 字符，上限 "
                    + aiConfig.getMaxAnalysisLength() + "）");
        }
        return content;
    }

    private void saveAnalysis(Integer questionId, String analysis) {
        QuestionBank update = new QuestionBank();
        update.setId(questionId);
        update.setAnalysis(analysis);
        update.setAnalysis_status(STATUS_DONE);
        update.setAnalysis_update_time(new Date());
        questionBankMapper.updateAnalysisById(update);
    }

    private QuestionBank loadQuestion(Integer questionId) throws AiException {
        if (questionId == null) {
            throw new AiException("参数错误：questionId 不能为空");
        }
        List<QuestionBank> list = questionBankMapper.getQuestionBankById(questionId);
        if (list == null || list.isEmpty()) {
            throw new AiException("题目不存在，id=" + questionId);
        }
        return list.get(0);
    }

    /** Mock 占位解析，仅用于框架演示与前端联调 */
    private String mockAnalysis(QuestionBank question) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Mock] AI 未启用（ai.enabled=false），以下为占位解析。");
        sb.append("本题类型：").append(nullToEmpty(question.getTopic_type())).append("。");
        sb.append("解题时请先回顾题目涉及的知识点，再结合答案分步推导；");
        sb.append("配置有效的 ai.api-key 并设置 ai.enabled=true 后，此处将返回大模型生成的真实解析。");
        return sb.toString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
