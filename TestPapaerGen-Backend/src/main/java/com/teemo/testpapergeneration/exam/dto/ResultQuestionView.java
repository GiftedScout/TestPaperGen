package com.teemo.testpapergeneration.exam.dto;

/**
 * 结果页单题：逐题对错、正确答案、解析与原因。
 */
public class ResultQuestionView {

    private Integer questionHistoryId;
    private String topic;
    private String topicType;
    private Double score;
    private String yourAnswer;
    private String correctAnswer;
    private String judgeResult;
    private Double scoreAwarded;
    private String analysis;
    private String reason;
    private String chapter1;
    private String chapter2;
    private String label1;
    private String label2;

    public Integer getQuestionHistoryId() {
        return questionHistoryId;
    }

    public void setQuestionHistoryId(Integer questionHistoryId) {
        this.questionHistoryId = questionHistoryId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTopicType() {
        return topicType;
    }

    public void setTopicType(String topicType) {
        this.topicType = topicType;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getYourAnswer() {
        return yourAnswer;
    }

    public void setYourAnswer(String yourAnswer) {
        this.yourAnswer = yourAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getJudgeResult() {
        return judgeResult;
    }

    public void setJudgeResult(String judgeResult) {
        this.judgeResult = judgeResult;
    }

    public Double getScoreAwarded() {
        return scoreAwarded;
    }

    public void setScoreAwarded(Double scoreAwarded) {
        this.scoreAwarded = scoreAwarded;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getChapter1() {
        return chapter1;
    }

    public void setChapter1(String chapter1) {
        this.chapter1 = chapter1;
    }

    public String getChapter2() {
        return chapter2;
    }

    public void setChapter2(String chapter2) {
        this.chapter2 = chapter2;
    }

    public String getLabel1() {
        return label1;
    }

    public void setLabel1(String label1) {
        this.label1 = label1;
    }

    public String getLabel2() {
        return label2;
    }

    public void setLabel2(String label2) {
        this.label2 = label2;
    }
}
