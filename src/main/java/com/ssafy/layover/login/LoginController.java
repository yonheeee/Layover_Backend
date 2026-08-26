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
import org.springframework.web.util.UriComponentsBuilder;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.login.dto.LoginRequest;
import com.ssafy.layover.login.dto.LoginResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

            String successUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/login")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("needsProfile", needsProfile)
                    .build()
                    .encode()
                    .toUriString();
            response.sendRedirect(successUrl);
        } catch (RuntimeException e) {
            boolean withdrawn = "탈퇴한 회원입니다.".equals(e.getMessage());
            String errorCode = withdrawn ? "withdrawn" : "kakao_login_failed";
            log.warn("[KakaoLogin] callback failed (type={})", e.getClass().getSimpleName());
            String failureUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/login")
                    .queryParam("error", errorCode)
                    .build()
                    .encode()
                    .toUriString();
            response.sendRedirect(failureUrl);
        }
    }
}
