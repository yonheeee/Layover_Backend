package com.ssafy.layover.user;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.user.dto.UpdateNicknameRequest;
import com.ssafy.layover.user.dto.UpdatePasswordRequest;
import com.ssafy.layover.user.dto.UpdatePhoneRequest;
import com.ssafy.layover.user.dto.UserMeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(meService.getMe(userId)));
    }

    @PutMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateNicknameRequest req) {
        meService.updateNickname(userId, req.getUsername());
        return ResponseEntity.ok(ApiResponse.success("닉네임이 변경되었습니다.", null));
    }

    @PutMapping("/phone")
    public ResponseEntity<ApiResponse<Void>> updatePhone(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdatePhoneRequest req) {
        meService.updatePhone(userId, req.getPhone());
        return ResponseEntity.ok(ApiResponse.success("전화번호가 변경되었습니다.", null));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdatePasswordRequest req) {
        meService.updatePassword(userId, req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }
}
