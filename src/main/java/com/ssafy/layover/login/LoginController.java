package com.ssafy.layover.login;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.login.dto.LoginRequest;
import com.ssafy.layover.login.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;

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
    public RedirectView kakaoCallback(@RequestParam String code) {
        LoginResponse tokens = kakaoLoginService.processKakaoLogin(code);
        String redirectUrl = "http://localhost:5173/login"
                + "?accessToken=" + tokens.getAccessToken()
                + "&refreshToken=" + tokens.getRefreshToken();
        return new RedirectView(redirectUrl);
    }
}
