package com.teemo.testpapergeneration.exam.mapper;

import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.entity.QuestionGenHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 任务包 A 专用只读/治理查询。
 * 不改动既有 QuestionBankMapper / QuestionGenHistoryMapper，避免影响既有功能。
 */
@Mapper
public interface ExamPaperMapper {

    @Select("select * from QuestionGenHistory where test_paper_uid=#{uid} order by id asc")
    List<QuestionGenHistory> listHistoriesByPaperUid(@Param("uid") String uid);

    @Select("select * from QuestionGenHistory where id=#{id}")
    QuestionGenHistory getHistoryById(@Param("id") Integer id);

    @Select("select test_paper_name from TestPaperGenHistory where test_paper_uid=#{uid} limit 1")
    String getPaperName(@Param("uid") String uid);

    /**
     * 待治理题目：答案尚未人工确认。
     */
    @Select("<script>"
            + "select * from QuestionBank where answer_confirm_status &lt;&gt; 1 "
            + "<if test='keyword != null and keyword != \"\"'> and topic like concat('%', #{keyword}, '%')</if>"
            + "<if test='topicType != null and topicType != \"\"'> and topic_type = #{topicType}</if>"
            + " order by update_time desc, id desc limit 200"
            + "</script>")
    List<QuestionBank> listUnconfirmed(@Param("keyword") String keyword,
                                      @Param("topicType") String topicType);

    /**
     * 仅更新建议值，绝不覆盖标准答案。
     */
    @Update("update QuestionBank set answer_suggest=#{suggest}, answer_confidence=#{confidence}, "
            + "answer_suggest_source=#{source} where id=#{id}")
    int updateAnswerSuggestion(@Param("id") Integer id,
                               @Param("suggest") String suggest,
                               @Param("confidence") Double confidence,
                               @Param("source") String source);

    /**
     * 人工确认后生效：这是全流程唯一写标准答案的入口。
     */
    @Update("update QuestionBank set answer=#{answer}, answer_confirm_status=1, "
            + "answer_suggest=#{answer}, answer_confirmed_by=#{user}, answer_confirmed_time=#{time} "
            + "where id=#{id}")
    int confirmAnswer(@Param("id") Integer id,
                      @Param("answer") String answer,
                      @Param("user") String user,
                      @Param("time") Date time);
}
