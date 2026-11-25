package com.teemo.testpapergeneration.exam.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生名册查询。不改动既有 UserMapper。
 */
@Mapper
public interface ExamUserMapper {

    @Select("select username from User where enable=1 and user_role='user' order by username asc")
    List<String> listStudentUsernames();

    @Select("select count(*) from User where username=#{username} and enable=1 and user_role='user'")
    int countStudent(@Param("username") String username);
}
