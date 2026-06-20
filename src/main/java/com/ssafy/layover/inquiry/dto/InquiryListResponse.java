package com.ssafy.layover.inquiry.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryListResponse {
    private String id;
    private String title;
    private String status;
    private LocalDateTime createdAt;
}
