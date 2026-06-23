package com.ssafy.layover.course;

import com.ssafy.layover.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<CourseResponse>> generateCourses(
            @RequestBody CourseGenerateRequest req) {
        return ResponseEntity.ok(courseService.generateCourses(req));
    }

    @PostMapping("/regenerate")
    public ResponseEntity<CourseResponse> regenerateCourse(
            @RequestBody CourseRegenerateRequest req) {
        return ResponseEntity.ok(courseService.regenerateCourse(req));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveCourse(
            @AuthenticationPrincipal String userId,
            @RequestBody SaveCourseRequest req) {
        String courseId = courseService.saveCourse(userId, req);
        return ResponseEntity.ok(ApiResponse.success("코스가 저장되었습니다.", courseId));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<SavedCourseResponse>>> getMyCourses(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getMyCourses(userId)));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @AuthenticationPrincipal String userId,
            @PathVariable String courseId) {
        courseService.deleteCourse(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success("코스가 삭제되었습니다.", null));
    }
}
