package com.ssafy.layover.find.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class FindIdRequest {

    @NotBlank
    private String realName;

    @NotNull
    private LocalDate birthDate;

    @NotBlank
    private String phone;
}
