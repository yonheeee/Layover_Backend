package com.ssafy.layover.course;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CoursePlaceMapper {

    void insert(CoursePlace coursePlace);

    List<CoursePlace> findByCourseId(@Param("courseId") String courseId);
}
