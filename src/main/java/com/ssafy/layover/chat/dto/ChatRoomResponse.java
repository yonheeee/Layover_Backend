package com.ssafy.layover.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {

    private String id;
    private String otherUserId;
    private String otherUsername;
    private String otherProfileImage;
    private String lastMessage;
    private String lastMessageType;
    private String lastMessageSenderId;
    private int unreadCount;
    private LocalDateTime updatedAt;
}
