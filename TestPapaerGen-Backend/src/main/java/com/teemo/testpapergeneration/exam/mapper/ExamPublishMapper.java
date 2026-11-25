package com.teemo.testpapergeneration.exam.mapper;

import com.teemo.testpapergeneration.exam.entity.ExamPublish;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExamPublishMapper {

    @Insert("insert into exam_publish "
            + "(test_paper_uid, test_paper_name, publisher, target_scope, deadline, total_score, question_count, create_time) "
            + "values (#{test_paper_uid}, #{test_paper_name}, #{publisher}, #{target_scope}, #{deadline}, "
            + "#{total_score}, #{question_count}, #{create_time})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamPublish publish);

    @Select("select * from exam_publish where id=#{id}")
    ExamPublish getById(@Param("id") Integer id);

    @Select("select * from exam_publish where publisher=#{publisher} order by create_time desc, id desc")
    List<ExamPublish> listByPublisher(@Param("publisher") String publisher);

    @Select("select * from exam_publish order by create_time desc, id desc")
    List<ExamPublish> listAll();
}
