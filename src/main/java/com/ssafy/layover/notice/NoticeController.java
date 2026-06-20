package com.ssafy.layover.notice;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.notice.dto.NoticeDetailResponse;
import com.ssafy.layover.notice.dto.NoticeListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeListResponse>>> getNotices() {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotices()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNotice(@PathVariable String id) {
        return ResponseEntity.ok(noticeService.getNotice(id));
    }
}
