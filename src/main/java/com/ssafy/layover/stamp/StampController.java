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

    /**
     * 촬영 직전 위치 확인.
     *
     * <p>거리 판정 규칙을 서버 한 곳에만 두기 위한 엔드포인트다. 예전에는
     * 프론트엔드가 반경 100m를 하드코딩해 따로 판정했는데, 서버 설정과
     * 어긋나면 사용자가 촬영을 다 마친 뒤에야 거부당했다.
     *
     * <p>여기서 통과해도 저장할 때 같은 판정을 한 번 더 한다. 이 호출을
     * 건너뛰고 POST /api/stamps 를 직접 부를 수 있기 때문이다.
     */
    @PostMapping("/verify-location")
    public ResponseEntity<ApiResponse<Void>> verifyLocation(@RequestBody VerifyLocationRequest req) {
        stampService.verifyLocation(
                req.getPlaceId(), req.getLatitude(), req.getLongitude(), req.getAccuracy());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Stamp>>> getMyStamps(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(stampService.getMyStamps(userId)));
    }
}
