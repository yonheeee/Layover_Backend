package com.ssafy.layover.auth;

import com.ssafy.layover.auth.dto.RefreshRequest;
import com.ssafy.layover.auth.dto.RefreshResponse;
import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.jwt.JwtUtil;
import com.ssafy.layover.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.fail("유효하지 않은 리프레시 토큰입니다."));
        }

        // access token으로는 재발급받을 수 없다. 종류를 구분하지 않으면 짧은 만료 시간이
        // 무의미해지고, 유출된 access token 하나로 계속 갱신할 수 있게 된다.
        if (!jwtUtil.isTokenType(refreshToken, JwtUtil.TYPE_REFRESH)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.fail("리프레시 토큰이 아닙니다. 다시 로그인해주세요."));
        }

        String userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getDeletedAt() != null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.fail("존재하지 않는 사용자입니다."));
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getRole());
        return ResponseEntity.ok(ApiResponse.success(new RefreshResponse(newAccessToken)));
    }
}
