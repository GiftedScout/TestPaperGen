package com.teemo.testpapergeneration.exam.entity;

import java.util.Date;

/**
 * 某学生对某次发布的提交状态与得分。
 * 发布时每个目标学生一行（assigned），提交后更新为 submitted。
 */
public class ExamSubmission {

    public static final String STATUS_ASSIGNED = "assigned";
    public static final String STATUS_SUBMITTED = "submitted";

    private Integer id;
    private Integer publish_id;
    private String student_username;
    private String status;
    private Date submit_time;
    private Double total_score;
    private Double score;
    private Integer pending_count;
    private Integer ungraded_count;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPublish_id() {
        return publish_id;
    }

    public void setPublish_id(Integer publish_id) {
        this.publish_id = publish_id;
    }

    public String getStudent_username() {
        return student_username;
    }

    public void setStudent_username(String student_username) {
        this.student_username = student_username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getSubmit_time() {
        return submit_time;
    }

    public void setSubmit_time(Date submit_time) {
        this.submit_time = submit_time;
    }

    public Double getTotal_score() {
        return total_score;
    }

    public void setTotal_score(Double total_score) {
        this.total_score = total_score;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getPending_count() {
        return pending_count;
    }

    public void setPending_count(Integer pending_count) {
        this.pending_count = pending_count;
    }

    public Integer getUngraded_count() {
        return ungraded_count;
    }

    public void setUngraded_count(Integer ungraded_count) {
        this.ungraded_count = ungraded_count;
    }
}
