package com.ssafy.layover.community.post.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostUpdateRequest {
    private String title;
    private String content;
    private String category;
}
