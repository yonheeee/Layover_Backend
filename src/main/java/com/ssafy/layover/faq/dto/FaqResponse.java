package com.ssafy.layover.faq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FaqResponse {
    private String id;
    private String question;
    private String answer;
    private LocalDateTime createdAt;
}
