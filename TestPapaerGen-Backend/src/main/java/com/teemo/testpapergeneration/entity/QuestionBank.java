package com.teemo.testpapergeneration.entity;

import java.util.Date;

public class QuestionBank {
    private Integer id;
    private String topic;
    private Integer topic_material_id;
    private String answer;
    private String topic_type;
    private Double score;
    private Integer difficulty;
    private String chapter_1;
    private String chapter_2;
    private String label_1;
    private String label_2;
    private Date update_time;
    // AI 解析字段，见 docs/04-数据库变更.md
    /** AI 生成的试题解析 */
    private String analysis;
    /** 解析状态：0 无解析，1 已生成 */
    private Integer analysis_status;
    /** 解析最近更新时间 */
    private Date analysis_update_time;
    // 答案治理字段，见 database/migrations/002_task_a_exam.sql
    /** 答案确认状态：0 未确认，1 已确认 */
    private Integer answer_confirm_status;
    /** AI 反推建议值（未生效） */
    private String answer_suggest;
    /** 建议置信度 0~1 */
    private Double answer_confidence;
    /** 建议来源：ai / mock */
    private String answer_suggest_source;
    /** 答案确认人 */
    private String answer_confirmed_by;
    /** 答案确认时间 */
    private Date answer_confirmed_time;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getTopic_material_id() {
        return topic_material_id;
    }

    public void setTopic_material_id(Integer topic_material_id) {
        this.topic_material_id = topic_material_id;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getTopic_type() {
        return topic_type;
    }

    public void setTopic_type(String topic_type) {
        this.topic_type = topic_type;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getChapter_1() {
        return chapter_1;
    }

    public void setChapter_1(String chapter_1) {
        this.chapter_1 = chapter_1;
    }

    public String getChapter_2() {
        return chapter_2;
    }

    public void setChapter_2(String chapter_2) {
        this.chapter_2 = chapter_2;
    }

    public String getLabel_1() {
        return label_1;
    }

    public void setLabel_1(String label_1) {
        this.label_1 = label_1;
    }

    public String getLabel_2() {
        return label_2;
    }

    public void setLabel_2(String label_2) {
        this.label_2 = label_2;
    }

    public Date getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(Date update_time) {
        this.update_time = update_time;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public Integer getAnalysis_status() {
        return analysis_status;
    }

    public void setAnalysis_status(Integer analysis_status) {
        this.analysis_status = analysis_status;
    }

    public Date getAnalysis_update_time() {
        return analysis_update_time;
    }

    public void setAnalysis_update_time(Date analysis_update_time) {
        this.analysis_update_time = analysis_update_time;
    }

    public Integer getAnswer_confirm_status() {
        return answer_confirm_status;
    }

    public void setAnswer_confirm_status(Integer answer_confirm_status) {
        this.answer_confirm_status = answer_confirm_status;
    }

    public String getAnswer_suggest() {
        return answer_suggest;
    }

    public void setAnswer_suggest(String answer_suggest) {
        this.answer_suggest = answer_suggest;
    }

    public Double getAnswer_confidence() {
        return answer_confidence;
    }

    public void setAnswer_confidence(Double answer_confidence) {
        this.answer_confidence = answer_confidence;
    }

    public String getAnswer_suggest_source() {
        return answer_suggest_source;
    }

    public void setAnswer_suggest_source(String answer_suggest_source) {
        this.answer_suggest_source = answer_suggest_source;
    }

    public String getAnswer_confirmed_by() {
        return answer_confirmed_by;
    }

    public void setAnswer_confirmed_by(String answer_confirmed_by) {
        this.answer_confirmed_by = answer_confirmed_by;
    }

    public Date getAnswer_confirmed_time() {
        return answer_confirmed_time;
    }

    public void setAnswer_confirmed_time(Date answer_confirmed_time) {
        this.answer_confirmed_time = answer_confirmed_time;
    }
}
