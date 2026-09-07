package com.ssafy.layover.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateProfileInfoRequest {

    // 비밀번호 없는 계정(카카오)은 null 허용이므로 @NotBlank 를 붙이지 않는다
    private String currentPassword;

    @NotBlank
    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;

    @NotNull
    @Past
    private LocalDate birthDate;
}
