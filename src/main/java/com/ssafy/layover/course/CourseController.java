package com.ssafy.layover.course;

import com.ssafy.layover.course.dto.CourseGenerateRequest;
import com.ssafy.layover.course.dto.CourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // 홈 화면에서 조건 입력 → 코스 3개 생성 (DB 저장 없음)
    @PostMapping("/generate")
    public ResponseEntity<List<CourseResponse>> generateCourses(
            @RequestBody CourseGenerateRequest req) {
        return ResponseEntity.ok(courseService.generateCourses(req));
    }
}
