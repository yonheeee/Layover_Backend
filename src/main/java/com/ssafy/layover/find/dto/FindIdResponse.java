package com.ssafy.layover.find.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FindIdResponse {

    private String maskedEmail;
    private LocalDateTime createdAt;
}
