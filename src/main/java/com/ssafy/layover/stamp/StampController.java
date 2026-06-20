package com.ssafy.layover.stamp;

import com.ssafy.layover.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stamps")
@RequiredArgsConstructor
public class StampController {

    private final StampService stampService;

    @PostMapping
    public ResponseEntity<ApiResponse<StampResponse>> saveStamp(
            @AuthenticationPrincipal String userId,
            @RequestBody SaveStampRequest req) {
        StampResponse res = stampService.saveStamp(userId, req);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Stamp>>> getMyStamps(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(stampService.getMyStamps(userId)));
    }
}
