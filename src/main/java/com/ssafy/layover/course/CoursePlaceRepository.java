package com.ssafy.layover.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, String> {
    List<CoursePlace> findByCourseIdOrderByOrderIndex(String courseId);
}
