package com.ssafy.layover.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatEventResponse {

    private String roomId;
    private ChatMessageResponse message;
}
