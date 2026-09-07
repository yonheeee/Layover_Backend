package com.ssafy.layover.user;

import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.exception.DuplicateException;
import com.ssafy.layover.common.exception.NotFoundException;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.login.KakaoLoginService;
import com.ssafy.layover.user.dto.UserMeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
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

    /**
     * "비밀번호 확인 API 를 먼저 호출하고 통과하면 수정 API 호출" 방식은 수정 API 를
     * 직접 부르면 우회된다. 그래서 비밀번호를 이 요청 안에서 함께 검증한다.
     */
    public void updateProfileInfo(String userId, String currentPassword, String phone, LocalDate birthDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        if (user.getPasswordHash() != null) {
            if (currentPassword == null || !bCryptPasswordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
            }
        } else {
            log.info("[Me] 비밀번호 없는 계정의 프로필 정보 수정 (userId={})", userId);
        }
        userRepository.updateProfileInfo(userId, phone, birthDate);
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
