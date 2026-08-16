package com.ssafy.layover.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReportResponse {

    private String id;
    private String reportedUserId;
    private String reportedUsername;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
