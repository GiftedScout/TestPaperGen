package com.teemo.testpapergeneration.exam.entity;

import java.util.Date;

/**
 * 一次试卷发布：一张试卷发给一批学生，并记录发布者与截止时间。
 */
public class ExamPublish {

    private Integer id;
    private String test_paper_uid;
    private String test_paper_name;
    private String publisher;
    private String target_scope;
    private Date deadline;
    private Double total_score;
    private Integer question_count;
    private Date create_time;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTest_paper_uid() {
        return test_paper_uid;
    }

    public void setTest_paper_uid(String test_paper_uid) {
        this.test_paper_uid = test_paper_uid;
    }

    public String getTest_paper_name() {
        return test_paper_name;
    }

    public void setTest_paper_name(String test_paper_name) {
        this.test_paper_name = test_paper_name;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getTarget_scope() {
        return target_scope;
    }

    public void setTarget_scope(String target_scope) {
        this.target_scope = target_scope;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public Double getTotal_score() {
        return total_score;
    }

    public void setTotal_score(Double total_score) {
        this.total_score = total_score;
    }

    public Integer getQuestion_count() {
        return question_count;
    }

    public void setQuestion_count(Integer question_count) {
        this.question_count = question_count;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }
}
