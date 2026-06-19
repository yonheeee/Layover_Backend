package com.ssafy.layover.user;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        ApiResponse<Void> response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
