package com.ssafy.layover.community.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private String id;
    private String userId;
    private String username;
    private String content;
    private LocalDateTime createdAt;
}
