package com.ssafy.layover.report;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.report.dto.ReportCreateRequest;
import com.ssafy.layover.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReport(
            @AuthenticationPrincipal String userId,
            @RequestBody ReportCreateRequest request) {
        return ResponseEntity.ok(reportService.createReport(userId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getMyReports(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getMyReports(userId)));
    }
}
