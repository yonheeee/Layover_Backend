package com.ssafy.layover.user;

import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.exception.DuplicateException;
import com.ssafy.layover.common.exception.NotFoundException;
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
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        return UserMeResponse.from(user);
    }

    public void updateNickname(String userId, String username) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateException("이미 사용 중인 닉네임입니다.");
        }
        userRepository.updateUsername(userId, username);
    }

    public void updatePhone(String userId, String phone) {
        userRepository.updatePhone(userId, phone);
    }

    public void updateProfileImage(String userId, String profileImage) {
        userRepository.updateProfileImage(userId, normalizeProfileImage(profileImage));
    }

    public void updatePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        if (user.getPasswordHash() == null) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }
        if (!bCryptPasswordEncoder.matches(currentPassword, user.getPasswordHash())) {
            // RuntimeException을 던지면 GlobalExceptionHandler의 Exception 핸들러가 잡아
            // 500 "서버 오류가 발생했습니다."로 나갔다. 사용자 입력 오류는 400이어야 한다.
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        userRepository.updatePassword(userId, bCryptPasswordEncoder.encode(newPassword));
    }

    public void withdraw(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        userRepository.updateDeletedAt(userId, LocalDateTime.now());

        if (user.getKakaoId() != null) {
            try {
                kakaoLoginService.unlinkKakao(user.getKakaoId());
            } catch (Exception ignored) {
            }
        }
    }

    private String normalizeProfileImage(String profileImage) {
        if (profileImage == null || profileImage.isBlank()) {
            return null;
        }
        return profileImage.trim();
    }
}
