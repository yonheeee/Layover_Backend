package com.ssafy.layover.community.post.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MyPostResponse {

    private String id;
    private String title;
    private String category;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
}
