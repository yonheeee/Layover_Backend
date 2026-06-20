package com.ssafy.layover.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
