package com.teemo.testpapergeneration.exam.entity;

import java.util.Date;

/**
 * 答案治理审计记录：原值、建议值、置信度、确认人、确认时间、状态全留痕。
 */
public class QuestionAnswerAudit {

    public static final String STATUS_SUGGESTED = "suggested";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_FAILED = "failed";

    private Integer id;
    private Integer question_bank_id;
    private String topic_type;
    private String old_answer;
    private String suggested_answer;
    private String confirmed_answer;
    private Double confidence;
    private String source;
    private String status;
    private String fail_reason;
    private String confirm_user;
    private Date confirm_time;
    private Date create_time;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuestion_bank_id() {
        return question_bank_id;
    }

    public void setQuestion_bank_id(Integer question_bank_id) {
        this.question_bank_id = question_bank_id;
    }

    public String getTopic_type() {
        return topic_type;
    }

    public void setTopic_type(String topic_type) {
        this.topic_type = topic_type;
    }

    public String getOld_answer() {
        return old_answer;
    }

    public void setOld_answer(String old_answer) {
        this.old_answer = old_answer;
    }

    public String getSuggested_answer() {
        return suggested_answer;
    }

    public void setSuggested_answer(String suggested_answer) {
        this.suggested_answer = suggested_answer;
    }

    public String getConfirmed_answer() {
        return confirmed_answer;
    }

    public void setConfirmed_answer(String confirmed_answer) {
        this.confirmed_answer = confirmed_answer;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFail_reason() {
        return fail_reason;
    }

    public void setFail_reason(String fail_reason) {
        this.fail_reason = fail_reason;
    }

    public String getConfirm_user() {
        return confirm_user;
    }

    public void setConfirm_user(String confirm_user) {
        this.confirm_user = confirm_user;
    }

    public Date getConfirm_time() {
        return confirm_time;
    }

    public void setConfirm_time(Date confirm_time) {
        this.confirm_time = confirm_time;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }
}
