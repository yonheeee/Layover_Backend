package com.ssafy.layover.find;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.find.dto.FindIdRequest;
import com.ssafy.layover.find.dto.FindIdResponse;
import com.ssafy.layover.find.dto.FindPwEmailRequest;
import com.ssafy.layover.find.dto.FindPwResetRequest;
import com.ssafy.layover.find.dto.FindPwVerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/find")
@RequiredArgsConstructor
public class FindController {

    private final FindService findService;

    @PostMapping("/id")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(@Valid @RequestBody FindIdRequest request) {
        return ResponseEntity.ok(findService.findId(request));
    }

    @PostMapping("/password/email/send")
    public ResponseEntity<ApiResponse<?>> sendPasswordResetCode(@Valid @RequestBody FindPwEmailRequest request) {
        return ResponseEntity.ok(findService.sendPasswordResetCode(request.getEmail()));
    }

    @PostMapping("/password/email/verify")
    public ResponseEntity<ApiResponse<?>> verifyPasswordResetCode(@Valid @RequestBody FindPwVerifyRequest request) {
        return ResponseEntity.ok(findService.verifyPasswordResetCode(request.getEmail(), request.getCode()));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@Valid @RequestBody FindPwResetRequest request) {
        return ResponseEntity.ok(findService.resetPassword(request));
    }
}
