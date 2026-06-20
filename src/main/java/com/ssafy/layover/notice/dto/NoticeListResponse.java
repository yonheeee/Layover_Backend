package com.ssafy.layover.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeListResponse {
    private String id;
    private String title;
    private LocalDateTime createdAt;
}
