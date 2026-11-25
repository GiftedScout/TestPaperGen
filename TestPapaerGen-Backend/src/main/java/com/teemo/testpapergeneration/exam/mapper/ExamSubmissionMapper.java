package com.teemo.testpapergeneration.exam.mapper;

import com.teemo.testpapergeneration.exam.entity.ExamSubmission;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExamSubmissionMapper {

    @Insert("<script>"
            + "insert into exam_submission "
            + "(publish_id, student_username, status, total_score, score, pending_count, ungraded_count) values "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.publish_id}, #{item.student_username}, #{item.status}, #{item.total_score}, "
            + "#{item.score}, #{item.pending_count}, #{item.ungraded_count})"
            + "</foreach>"
            + "</script>")
    int batchInsert(@Param("list") List<ExamSubmission> list);

    @Select("select * from exam_submission where publish_id=#{publishId} and student_username=#{username}")
    ExamSubmission get(@Param("publishId") Integer publishId,
                       @Param("username") String username);

    @Select("select * from exam_submission where publish_id=#{publishId} order by student_username asc")
    List<ExamSubmission> listByPublish(@Param("publishId") Integer publishId);

    @Select("select * from exam_submission where student_username=#{username} order by id desc")
    List<ExamSubmission> listByStudent(@Param("username") String username);

    /**
     * 条件更新：只有 assigned 才能提交，天然防重复提交。
     */
    @Update("update exam_submission set status='submitted', submit_time=#{submit_time}, "
            + "score=#{score}, pending_count=#{pending_count}, ungraded_count=#{ungraded_count} "
            + "where id=#{id} and status='assigned'")
    int markSubmitted(ExamSubmission submission);

    @Select("select count(*) from exam_submission where publish_id=#{publishId}")
    int countTargets(@Param("publishId") Integer publishId);

    @Select("select count(*) from exam_submission where publish_id=#{publishId} and status='submitted'")
    int countSubmitted(@Param("publishId") Integer publishId);
}
