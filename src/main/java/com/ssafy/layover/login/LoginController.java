package com.ssafy.layover.login;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.login.dto.LoginRequest;
import com.ssafy.layover.login.dto.LoginResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService;
    private final KakaoLoginService kakaoLoginService;

    @Value("${app.frontend-url}")  // properties에서 값 읽어옴
    private String frontendUrl;

    @PostMapping
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    @GetMapping("/kakao")
    public ResponseEntity<ApiResponse<String>> kakaoLogin() {
        return ResponseEntity.ok(ApiResponse.success(kakaoLoginService.getKakaoAuthUrl()));
    }

    @GetMapping("/kakao/callback")
    public void kakaoCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = kakaoLoginService.processKakaoLogin(code);
            String accessToken = (String) result.get("accessToken");
            String refreshToken = (String) result.get("refreshToken");
            boolean needsProfile = (boolean) result.get("needsProfile");

response.sendRedirect(frontendUrl + "/login?accessToken=" + accessToken
        + "&refreshToken=" + refreshToken
        + "&needsProfile=" + needsProfile);
        } catch (RuntimeException e) {
            response.sendRedirect(frontendUrl + "/login?error=withdrawn");
        }
    }
}
