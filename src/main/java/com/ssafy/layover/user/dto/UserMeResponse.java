package com.ssafy.layover.user.dto;

import com.ssafy.layover.common.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserMeResponse {

    private String username;
    private String realName;
    private String email;
    private boolean kakao;
    private LocalDate birthDate;
    private String phone;
    private String profileImage;
    private int stampCount;
    private String role;

    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getUsername(),
                user.getRealName(),
                user.getEmail(),
                user.getKakaoId() != null && !user.getKakaoId().isBlank(),
                user.getBirthDate(),
                user.getPhone(),
                user.getProfileImage(),
                user.getStampCount(),
                user.getRole()
        );
    }
}
