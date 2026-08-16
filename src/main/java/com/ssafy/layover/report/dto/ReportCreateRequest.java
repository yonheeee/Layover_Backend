package com.ssafy.layover.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportCreateRequest {

    private String reportedUserId;
    private String content;
}
