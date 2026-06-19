package com.ssafy.layover.login;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.login.dto.LoginRequest;
import com.ssafy.layover.login.dto.LoginResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.*;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final KakaoLoginService kakaoLoginService;

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
        Map<String, Object> result = kakaoLoginService.processKakaoLogin(code);
        String accessToken = (String) result.get("accessToken");
        String refreshToken = (String) result.get("refreshToken");
        boolean needsProfile = (boolean) result.get("needsProfile");

        response.sendRedirect("http://localhost:5173/login?accessToken=" + accessToken
                + "&refreshToken=" + refreshToken
                + "&needsProfile=" + needsProfile);
    }
}
