package com.ssafy.layover.signup;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.signup.dto.EmailCheckRequest;
import com.ssafy.layover.signup.dto.EmailSendRequest;
import com.ssafy.layover.signup.dto.EmailVerifyRequest;
import com.ssafy.layover.signup.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<?>> checkEmail(@Valid @RequestBody EmailCheckRequest request) {
        return ResponseEntity.ok(signupService.checkEmail(request.getEmail()));
    }

    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<?>> sendEmailCode(@Valid @RequestBody EmailSendRequest request) {
        return ResponseEntity.ok(signupService.sendEmailCode(request.getEmail()));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<?>> verifyEmailCode(@Valid @RequestBody EmailVerifyRequest request) {
        return ResponseEntity.ok(signupService.verifyEmailCode(request.getEmail(), request.getCode()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(signupService.signup(request));
    }
}
