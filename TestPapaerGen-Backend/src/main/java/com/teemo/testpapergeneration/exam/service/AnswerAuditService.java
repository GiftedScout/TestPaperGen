package com.teemo.testpapergeneration.exam.service;

import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.ai.AiClient;
import com.teemo.testpapergeneration.ai.AiConfig;
import com.teemo.testpapergeneration.ai.AiException;
import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.exam.entity.QuestionAnswerAudit;
import com.teemo.testpapergeneration.exam.grading.AnswerJudge;
import com.teemo.testpapergeneration.exam.mapper.ExamPaperMapper;
import com.teemo.testpapergeneration.exam.mapper.QuestionAnswerAuditMapper;
import com.teemo.testpapergeneration.mapper.QuestionBankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 答案数据治理：AI 只产出建议，人工确认后才生效，全过程留痕。
 */
@Service
public class AnswerAuditService {

    private static final double LOW_CONFIDENCE = 0.9;
    private static final double ACCURACY_THRESHOLD = 90.0;
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final ExamPaperMapper paperMapper;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionAnswerAuditMapper auditMapper;
    private final AnswerJudge answerJudge;
    private final AiConfig aiConfig;
    private final AiClient aiClient;

    public AnswerAuditService(ExamPaperMapper paperMapper,
                              QuestionBankMapper questionBankMapper,
                              QuestionAnswerAuditMapper auditMapper,
                              AnswerJudge answerJudge,
                              AiConfig aiConfig,
                              AiClient aiClient) {
        this.paperMapper = paperMapper;
        this.questionBankMapper = questionBankMapper;
        this.auditMapper = auditMapper;
        this.answerJudge = answerJudge;
        this.aiConfig = aiConfig;
        this.aiClient = aiClient;
    }

    public List<JSONObject> pending(String keyword, String topicType, boolean lowOnly) {
        List<QuestionBank> questions = paperMapper.listUnconfirmed(keyword, topicType);
        Map<Integer, QuestionAnswerAudit> latest = new HashMap<>();
        for (QuestionAnswerAudit audit : auditMapper.latestAll()) {
            latest.put(audit.getQuestion_bank_id(), audit);
        }
        List<JSONObject> result = new ArrayList<>();
        for (QuestionBank question : questions) {
            QuestionAnswerAudit audit = latest.get(question.getId());
            Double confidence = audit != null ? audit.getConfidence() : question.getAnswer_confidence();
            String suggested = audit != null ? audit.getSuggested_answer() : question.getAnswer_suggest();
            String source = audit != null ? audit.getSource() : question.getAnswer_suggest_source();
            String status = audit != null ? audit.getStatus() : null;
            boolean low = confidence != null && confidence < LOW_CONFIDENCE;
            if (lowOnly && !low) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("questionId", question.getId());
            item.put("topic", question.getTopic());
            item.put("topicType", question.getTopic_type());
            item.put("currentAnswer", question.getAnswer());
            item.put("answerConfirmStatus", question.getAnswer_confirm_status());
            item.put("latestAuditId", audit == null ? null : audit.getId());
            item.put("suggestedAnswer", suggested);
            item.put("confidence", confidence);
            item.put("source", source);
            item.put("auditStatus", status);
            item.put("failReason", audit == null ? null : audit.getFail_reason());
            item.put("lowConfidence", low);
            item.put("createTime", audit == null ? null : formatDate(audit.getCreate_time()));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public List<JSONObject> suggest(List<Integer> questionIds, String operator) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw ExamException.badRequest("请选择要反推的题目");
        }
        List<JSONObject> result = new ArrayList<>();
        for (Integer questionId : questionIds) {
            QuestionBank question = bankOf(questionId);
            if (question == null) {
                JSONObject item = new JSONObject();
                item.put("questionId", questionId);
                item.put("status", QuestionAnswerAudit.STATUS_FAILED);
                item.put("failReason", "题目不存在");
                result.add(item);
                continue;
            }
            if (question.getAnswer_confirm_status() != null && question.getAnswer_confirm_status() == 1) {
                result.add(saveFailed(question, "答案已确认，无需反推", null));
                continue;
            }
            AnswerJudge.Category category = answerJudge.categoryOf(question.getTopic_type());
            if (category == AnswerJudge.Category.SUBJECTIVE) {
                result.add(saveFailed(question, "主观题不支持自动反推答案", null));
                continue;
            }
            try {
                Suggestion suggestion = aiConfig.isEnabled()
                        ? parseAiSuggestion(aiClient.suggestAnswer(question))
                        : mockSuggestion(question);
                String normalized = answerJudge.validateSuggestion(question.getTopic_type(), suggestion.answer);

                QuestionAnswerAudit audit = new QuestionAnswerAudit();
                audit.setQuestion_bank_id(question.getId());
                audit.setTopic_type(question.getTopic_type());
                audit.setOld_answer(question.getAnswer());
                audit.setSuggested_answer(normalized);
                audit.setConfidence(suggestion.confidence);
                audit.setSource(suggestion.source);
                audit.setStatus(QuestionAnswerAudit.STATUS_SUGGESTED);
                audit.setCreate_time(new Date());
                auditMapper.insert(audit);

                paperMapper.updateAnswerSuggestion(question.getId(), normalized, suggestion.confidence, suggestion.source);

                JSONObject item = new JSONObject();
                item.put("questionId", question.getId());
                item.put("auditId", audit.getId());
                item.put("status", audit.getStatus());
                item.put("suggestedAnswer", normalized);
                item.put("confidence", suggestion.confidence);
                item.put("source", suggestion.source);
                item.put("lowConfidence", suggestion.confidence < LOW_CONFIDENCE);
                result.add(item);
            } catch (AiException e) {
                result.add(saveFailed(question, "AI 调用失败：" + e.getMessage(), "ai"));
            } catch (IllegalArgumentException e) {
                result.add(saveFailed(question, "建议格式非法：" + e.getMessage(), aiConfig.isEnabled() ? "ai" : "mock"));
            }
        }
        return result;
    }

    @Transactional
    public JSONObject confirm(Integer auditId, String finalAnswer, String operator) {
        QuestionAnswerAudit audit = auditMapper.getById(auditId);
        if (audit == null) {
            throw ExamException.badRequest("审计记录不存在");
        }
        if (!QuestionAnswerAudit.STATUS_SUGGESTED.equals(audit.getStatus())) {
            throw ExamException.badRequest("该建议已被处理，无法重复确认");
        }
        QuestionBank question = bankOf(audit.getQuestion_bank_id());
        if (question == null) {
            throw ExamException.badRequest("题目不存在");
        }
        String normalized = answerJudge.validateSuggestion(audit.getTopic_type(), finalAnswer);
        Date now = new Date();
        int updated = auditMapper.markConfirmed(auditId, normalized, operator, now);
        if (updated == 0) {
            throw ExamException.badRequest("该建议已被处理，无法重复确认");
        }
        paperMapper.confirmAnswer(question.getId(), normalized, operator, now);

        JSONObject result = new JSONObject();
        result.put("questionId", question.getId());
        result.put("auditId", auditId);
        result.put("answer", normalized);
        result.put("confirmUser", operator);
        result.put("confirmTime", formatDate(now));
        return result;
    }

    @Transactional
    public JSONObject reject(Integer auditId, String reason, String operator) {
        QuestionAnswerAudit audit = auditMapper.getById(auditId);
        if (audit == null) {
            throw ExamException.badRequest("审计记录不存在");
        }
        Date now = new Date();
        int updated = auditMapper.markRejected(auditId, isBlank(reason) ? "人工驳回" : reason, operator, now);
        if (updated == 0) {
            throw ExamException.badRequest("该建议已被处理，无法重复驳回");
        }
        JSONObject result = new JSONObject();
        result.put("auditId", auditId);
        result.put("status", QuestionAnswerAudit.STATUS_REJECTED);
        return result;
    }

    public List<JSONObject> audits(Integer questionId) {
        List<JSONObject> result = new ArrayList<>();
        for (QuestionAnswerAudit audit : auditMapper.listByQuestion(questionId)) {
            JSONObject item = new JSONObject();
            item.put("id", audit.getId());
            item.put("questionId", audit.getQuestion_bank_id());
            item.put("topicType", audit.getTopic_type());
            item.put("oldAnswer", audit.getOld_answer());
            item.put("suggestedAnswer", audit.getSuggested_answer());
            item.put("confirmedAnswer", audit.getConfirmed_answer());
            item.put("confidence", audit.getConfidence());
            item.put("source", audit.getSource());
            item.put("status", audit.getStatus());
            item.put("failReason", audit.getFail_reason());
            item.put("confirmUser", audit.getConfirm_user());
            item.put("confirmTime", formatDate(audit.getConfirm_time()));
            item.put("createTime", formatDate(audit.getCreate_time()));
            result.add(item);
        }
        return result;
    }

    /**
     * 抽检准确率：以「人工确认值与建议值一致」作为反推正确，按题型分层统计。
     */
    public JSONObject sampling() {
        Map<String, int[]> stats = new LinkedHashMap<>();
        stats.put("选择题", new int[2]);
        stats.put("判断题", new int[2]);
        stats.put("填空题", new int[2]);
        for (QuestionAnswerAudit audit : auditMapper.listConfirmed()) {
            String type = normalizeType(audit.getTopic_type());
            if (type == null) {
                continue;
            }
            int[] counter = stats.get(type);
            counter[0]++;
            if (audit.getSuggested_answer() != null
                    && answerJudge.equivalent(audit.getTopic_type(), audit.getSuggested_answer(), audit.getConfirmed_answer())) {
                counter[1]++;
            }
        }
        List<JSONObject> types = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : stats.entrySet()) {
            int total = entry.getValue()[0];
            int unchanged = entry.getValue()[1];
            Double accuracy = total == 0 ? null : Math.round((double) unchanged / total * 10000.0) / 100.0;
            JSONObject item = new JSONObject();
            item.put("topicType", entry.getKey());
            item.put("confirmedCount", total);
            item.put("unchangedCount", unchanged);
            item.put("accuracy", accuracy);
            item.put("pass", accuracy != null && accuracy >= ACCURACY_THRESHOLD);
            types.add(item);
        }
        JSONObject result = new JSONObject();
        result.put("threshold", ACCURACY_THRESHOLD);
        result.put("method", "按题型分层抽检：人工确认值与 AI 建议值一致计为反推正确；"
                + "确认时修改过答案计为反推错误。准确率低于 90% 的题型需回退人工补齐。");
        result.put("types", types);
        return result;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private QuestionBank bankOf(Integer questionId) {
        if (questionId == null) {
            return null;
        }
        List<QuestionBank> banks = questionBankMapper.getQuestionBankById(questionId);
        return banks == null || banks.isEmpty() ? null : banks.get(0);
    }

    private JSONObject saveFailed(QuestionBank question, String reason, String source) {
        QuestionAnswerAudit audit = new QuestionAnswerAudit();
        audit.setQuestion_bank_id(question.getId());
        audit.setTopic_type(question.getTopic_type());
        audit.setOld_answer(question.getAnswer());
        audit.setSource(source);
        audit.setStatus(QuestionAnswerAudit.STATUS_FAILED);
        audit.setFail_reason(brief(reason));
        audit.setCreate_time(new Date());
        auditMapper.insert(audit);

        JSONObject item = new JSONObject();
        item.put("questionId", question.getId());
        item.put("auditId", audit.getId());
        item.put("status", audit.getStatus());
        item.put("failReason", audit.getFail_reason());
        return item;
    }

    private Suggestion mockSuggestion(QuestionBank question) {
        AnswerJudge.Category category = answerJudge.categoryOf(question.getTopic_type());
        String answer;
        if (category == AnswerJudge.Category.CHOICE) {
            answer = "A";
        } else if (category == AnswerJudge.Category.JUDGE) {
            answer = "对";
        } else {
            int blanks = answerJudge.blankCount(question.getTopic(), question.getAnswer(), false);
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < blanks; i++) {
                parts.add("待补充");
            }
            answer = String.join("|", parts);
        }
        return new Suggestion(answer, 0.30, "mock");
    }

    private Suggestion parseAiSuggestion(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                JSONObject json = JSONObject.parseObject(text.substring(start, end + 1));
                String answer = json.getString("answer");
                Double confidence = json.getDouble("confidence");
                if (answer != null && !answer.trim().isEmpty()) {
                    return new Suggestion(answer.trim(), clamp(confidence == null ? 0.5 : confidence), "ai");
                }
            } catch (Exception ignore) {
                // 落到下面的宽松解析
            }
        }
        return new Suggestion(text, 0.30, "ai");
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 1);
    }

    private String normalizeType(String topicType) {
        AnswerJudge.Category category = answerJudge.categoryOf(topicType);
        if (category == AnswerJudge.Category.CHOICE) {
            return "选择题";
        }
        if (category == AnswerJudge.Category.JUDGE) {
            return "判断题";
        }
        if (category == AnswerJudge.Category.FILL) {
            return "填空题";
        }
        return null;
    }

    private String formatDate(Date date) {
        return date == null ? null : new SimpleDateFormat(DATE_PATTERN).format(date);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String brief(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 240 ? value.substring(0, 240) + "..." : value;
    }

    private static class Suggestion {
        private final String answer;
        private final double confidence;
        private final String source;

        private Suggestion(String answer, double confidence, String source) {
            this.answer = answer;
            this.confidence = confidence;
            this.source = source;
        }
    }
}
