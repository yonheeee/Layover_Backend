package com.ssafy.layover.find.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindPwVerifyRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;
}
