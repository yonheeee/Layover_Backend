package com.ssafy.layover.user;

import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.login.KakaoLoginService;
import com.ssafy.layover.user.dto.UserMeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final KakaoLoginService kakaoLoginService;

    public UserMeResponse getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return UserMeResponse.from(user);
    }

    public void updateNickname(String userId, String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        userRepository.updateUsername(userId, username);
    }

    public void updatePhone(String userId, String phone) {
        userRepository.updatePhone(userId, phone);
    }

    public void updatePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        if (!bCryptPasswordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("현재 비밀번호가 올바르지 않습니다.");
        }
        userRepository.updatePassword(userId, bCryptPasswordEncoder.encode(newPassword));
    }

    public void withdraw(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        userRepository.updateDeletedAt(userId, LocalDateTime.now());

        if (user.getKakaoId() != null) {
            try {
                kakaoLoginService.unlinkKakao(user.getKakaoId());
            } catch (Exception ignored) {
            }
        }
    }
}
