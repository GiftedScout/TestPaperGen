package com.teemo.testpapergeneration.exam.mapper;

import com.teemo.testpapergeneration.exam.entity.QuestionAnswerAudit;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

@Mapper
public interface QuestionAnswerAuditMapper {

    @Insert("insert into question_answer_audit "
            + "(question_bank_id, topic_type, old_answer, suggested_answer, confirmed_answer, confidence, source, "
            + "status, fail_reason, confirm_user, confirm_time, create_time) values "
            + "(#{question_bank_id}, #{topic_type}, #{old_answer}, #{suggested_answer}, #{confirmed_answer}, "
            + "#{confidence}, #{source}, #{status}, #{fail_reason}, #{confirm_user}, #{confirm_time}, #{create_time})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionAnswerAudit audit);

    @Select("select * from question_answer_audit where id=#{id}")
    QuestionAnswerAudit getById(@Param("id") Integer id);

    /**
     * 每个题目最近一次审计记录，用于待治理列表与低置信度提示。
     */
    @Select("select * from question_answer_audit a "
            + "where a.id = (select max(b.id) from question_answer_audit b where b.question_bank_id = a.question_bank_id)")
    List<QuestionAnswerAudit> latestAll();

    @Select("select * from question_answer_audit where question_bank_id=#{questionId} "
            + "order by create_time desc, id desc")
    List<QuestionAnswerAudit> listByQuestion(@Param("questionId") Integer questionId);

    @Update("update question_answer_audit set status='confirmed', confirmed_answer=#{answer}, "
            + "confirm_user=#{user}, confirm_time=#{time} where id=#{id} and status='suggested'")
    int markConfirmed(@Param("id") Integer id,
                      @Param("answer") String answer,
                      @Param("user") String user,
                      @Param("time") Date time);

    @Update("update question_answer_audit set status='rejected', fail_reason=#{reason}, "
            + "confirm_user=#{user}, confirm_time=#{time} where id=#{id} and status='suggested'")
    int markRejected(@Param("id") Integer id,
                     @Param("reason") String reason,
                     @Param("user") String user,
                     @Param("time") Date time);

    @Select("select * from question_answer_audit where status='confirmed' order by id asc")
    List<QuestionAnswerAudit> listConfirmed();
}
