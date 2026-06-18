package com.ssafy.layover.login;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.jwt.JwtUtil;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.login.dto.LoginRequest;
import com.ssafy.layover.login.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public ApiResponse<LoginResponse> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !bCryptPasswordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ApiResponse.fail("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if (user.getDeletedAt() != null) {
            return ApiResponse.fail("탈퇴한 회원입니다.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return ApiResponse.success(new LoginResponse(accessToken, refreshToken));
    }
}
