package com.ssafy.layover.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping("/generate")
    public ResponseEntity<List<CourseResponse>> generateCourses(
            @RequestBody CourseGenerateRequest req) {
        return ResponseEntity.ok(courseService.generateCourses(req));
    }
}
