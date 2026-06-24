package com.ssafy.layover.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class InquiryCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private List<String> attachmentUrls;
}
