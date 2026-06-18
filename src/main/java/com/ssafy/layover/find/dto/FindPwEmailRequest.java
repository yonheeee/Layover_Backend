package com.ssafy.layover.find.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindPwEmailRequest {

    @NotBlank
    @Email
    private String email;
}
