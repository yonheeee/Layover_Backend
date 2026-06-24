package com.ssafy.layover.inquiry.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
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

    @JsonIgnore
    private String attachmentUrlsStr;

    public List<String> getAttachmentUrls() {
        if (attachmentUrlsStr == null || attachmentUrlsStr.isBlank()) return List.of();
        return Arrays.asList(attachmentUrlsStr.split(","));
    }
}
