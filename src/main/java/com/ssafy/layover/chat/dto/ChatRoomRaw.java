package com.ssafy.layover.chat.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatRoomRaw {

    private String roomId;
    private String otherUserId;
    private String otherUsername;
    private String otherProfileImage;
    private String lastMessageEncryptedContent;
    private String lastMessageIv;
    private String lastMessageType;
    private String lastMessageSenderId;
    private int unreadCount;
    private LocalDateTime updatedAt;
}
