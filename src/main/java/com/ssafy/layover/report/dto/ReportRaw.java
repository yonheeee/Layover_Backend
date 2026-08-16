package com.ssafy.layover.report.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReportRaw {

    private String id;
    private String reportedUserId;
    private String reportedUsername;
    private String encryptedContent;
    private String iv;
    private String status;
    private LocalDateTime createdAt;
}
