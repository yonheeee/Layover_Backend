package com.ssafy.layover.course;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseMapper {

    void insert(Course course);

    List<Course> findByUserId(@Param("userId") String userId);

    List<Course> findAllPublic();
}
