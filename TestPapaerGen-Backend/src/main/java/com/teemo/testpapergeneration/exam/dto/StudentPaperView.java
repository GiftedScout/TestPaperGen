package com.teemo.testpapergeneration.exam.dto;

import java.util.List;

/**
 * 学生答题页数据。严禁包含答案与解析字段。
 */
public class StudentPaperView {

    private Integer publishId;
    private String paperName;
    private String publisher;
    private String deadline;
    private boolean submitted;
    private Double totalScore;
    private Integer questionCount;
    private List<StudentQuestionView> questions;

    public Integer getPublishId() {
        return publishId;
    }

    public void setPublishId(Integer publishId) {
        this.publishId = publishId;
    }

    public String getPaperName() {
        return paperName;
    }

    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public List<StudentQuestionView> getQuestions() {
        return questions;
    }

    public void setQuestions(List<StudentQuestionView> questions) {
        this.questions = questions;
    }
}
