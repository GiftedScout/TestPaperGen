package com.teemo.testpapergeneration.exam.dto;

import java.util.List;

/**
 * 学生结果页：提交后才允许返回，包含逐题正确答案与解析。
 */
public class StudentResultView {

    private Integer publishId;
    private String paperName;
    private Double score;
    private Double totalScore;
    private Double objectivePossible;
    private Integer pendingCount;
    private Integer ungradedCount;
    private List<ResultQuestionView> questions;

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

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public Double getObjectivePossible() {
        return objectivePossible;
    }

    public void setObjectivePossible(Double objectivePossible) {
        this.objectivePossible = objectivePossible;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getUngradedCount() {
        return ungradedCount;
    }

    public void setUngradedCount(Integer ungradedCount) {
        this.ungradedCount = ungradedCount;
    }

    public List<ResultQuestionView> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ResultQuestionView> questions) {
        this.questions = questions;
    }
}
