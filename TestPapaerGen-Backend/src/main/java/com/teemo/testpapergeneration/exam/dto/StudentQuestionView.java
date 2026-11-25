package com.teemo.testpapergeneration.exam.dto;

/**
 * 答题页单题视图：只有题干与作答所需信息，没有答案、解析。
 */
public class StudentQuestionView {

    private Integer questionHistoryId;
    private String topic;
    private String topicType;
    private Double score;
    private String chapter1;
    private String chapter2;
    private String label1;
    private String label2;
    private Integer blankCount;

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

    public Integer getBlankCount() {
        return blankCount;
    }

    public void setBlankCount(Integer blankCount) {
        this.blankCount = blankCount;
    }
}
