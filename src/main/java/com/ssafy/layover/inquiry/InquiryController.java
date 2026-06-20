package com.ssafy.layover.inquiry;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.inquiry.dto.InquiryCreateRequest;
import com.ssafy.layover.inquiry.dto.InquiryDetailResponse;
import com.ssafy.layover.inquiry.dto.InquiryListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createInquiry(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody InquiryCreateRequest req) {
        return ResponseEntity.ok(inquiryService.createInquiry(userId, req));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InquiryListResponse>>> getMyInquiries(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getMyInquiries(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getInquiry(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(inquiryService.getInquiry(id, userId));
    }
}
