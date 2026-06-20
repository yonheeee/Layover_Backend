package com.ssafy.layover.community.post.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {
    private String category;
    private String title;
    private String content;
}
