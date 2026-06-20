package com.ssafy.layover.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeDetailResponse {
    private String id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
