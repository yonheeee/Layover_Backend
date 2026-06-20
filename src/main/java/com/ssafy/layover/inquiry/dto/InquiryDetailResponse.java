package com.ssafy.layover.inquiry.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryDetailResponse {
    private String id;
    private String title;
    private String content;
    private String status;
    private String answer;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;
}
