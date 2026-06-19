package com.ssafy.layover.user;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public ApiResponse<Void> updateProfile(String userId, UpdateProfileRequest request) {
        if (!userRepository.existsById(userId)) {
            return ApiResponse.fail("사용자를 찾을 수 없습니다.");
        }
        userRepository.updateProfile(userId, request.getName(), request.getBirthDate(), request.getPhone());
        return ApiResponse.success("프로필이 업데이트되었습니다.", null);
    }
}
