package com.ssafy.layover.chat.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessageRaw {

    private String id;
    private String roomId;
    private String senderId;
    private String senderUsername;
    private String type;
    private String encryptedContent;
    private String iv;
    private LocalDateTime createdAt;
}
