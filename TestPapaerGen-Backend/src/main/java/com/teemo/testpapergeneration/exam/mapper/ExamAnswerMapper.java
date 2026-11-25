package com.teemo.testpapergeneration.exam.mapper;

import com.teemo.testpapergeneration.exam.entity.ExamAnswer;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamAnswerMapper {

    @Insert("<script>"
            + "insert into exam_answer "
            + "(submission_id, question_history_id, question_bank_id, answer_raw, judge_result, judge_mode, score_awarded, judge_time) values "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.submission_id}, #{item.question_history_id}, #{item.question_bank_id}, #{item.answer_raw}, "
            + "#{item.judge_result}, #{item.judge_mode}, #{item.score_awarded}, #{item.judge_time})"
            + "</foreach>"
            + "</script>")
    int batchInsert(@Param("list") List<ExamAnswer> list);

    @Select("select * from exam_answer where submission_id=#{submissionId} order by question_history_id asc")
    List<ExamAnswer> listBySubmission(@Param("submissionId") Integer submissionId);
}
