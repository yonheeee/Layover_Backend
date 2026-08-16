package com.ssafy.layover.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {

    private String id;
    private String roomId;
    private String senderId;
    private String senderUsername;
    private String type;
    private String content;
    private LocalDateTime createdAt;
}
