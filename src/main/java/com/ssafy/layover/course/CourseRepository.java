package com.ssafy.layover.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Course> findByIsPublicTrueOrderByCreatedAtDesc();
}
