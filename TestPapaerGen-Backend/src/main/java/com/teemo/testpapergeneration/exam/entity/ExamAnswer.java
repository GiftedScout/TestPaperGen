package com.teemo.testpapergeneration.exam.entity;

import java.util.Date;

/**
 * 某学生对某道题的作答与判分结果。
 * 同一学生对同一题只保留一份有效作答（唯一约束 submission_id + question_history_id）。
 */
public class ExamAnswer {

    public static final String RESULT_CORRECT = "correct";
    public static final String RESULT_WRONG = "wrong";
    public static final String RESULT_PENDING = "pending";
    public static final String RESULT_UNGRADED = "ungraded";

    public static final String MODE_AUTO = "auto";
    public static final String MODE_PENDING = "pending";

    private Integer id;
    private Integer submission_id;
    private Integer question_history_id;
    private Integer question_bank_id;
    private String answer_raw;
    private String judge_result;
    private String judge_mode;
    private Double score_awarded;
    private Date judge_time;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubmission_id() {
        return submission_id;
    }

    public void setSubmission_id(Integer submission_id) {
        this.submission_id = submission_id;
    }

    public Integer getQuestion_history_id() {
        return question_history_id;
    }

    public void setQuestion_history_id(Integer question_history_id) {
        this.question_history_id = question_history_id;
    }

    public Integer getQuestion_bank_id() {
        return question_bank_id;
    }

    public void setQuestion_bank_id(Integer question_bank_id) {
        this.question_bank_id = question_bank_id;
    }

    public String getAnswer_raw() {
        return answer_raw;
    }

    public void setAnswer_raw(String answer_raw) {
        this.answer_raw = answer_raw;
    }

    public String getJudge_result() {
        return judge_result;
    }

    public void setJudge_result(String judge_result) {
        this.judge_result = judge_result;
    }

    public String getJudge_mode() {
        return judge_mode;
    }

    public void setJudge_mode(String judge_mode) {
        this.judge_mode = judge_mode;
    }

    public Double getScore_awarded() {
        return score_awarded;
    }

    public void setScore_awarded(Double score_awarded) {
        this.score_awarded = score_awarded;
    }

    public Date getJudge_time() {
        return judge_time;
    }

    public void setJudge_time(Date judge_time) {
        this.judge_time = judge_time;
    }
}
