package com.ssafy.layover.chat;

import com.ssafy.layover.chat.dto.ChatMessageRequest;
import com.ssafy.layover.chat.dto.ChatMessageResponse;
import com.ssafy.layover.chat.dto.ChatRoomCreateRequest;
import com.ssafy.layover.chat.dto.ChatRoomResponse;
import com.ssafy.layover.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatSseService chatSseService;

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetRoom(
            @AuthenticationPrincipal String userId,
            @RequestBody ChatRoomCreateRequest request) {
        return ResponseEntity.ok(chatService.createOrGetRoom(userId, request.getUserId()));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRooms(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getRooms(userId)));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(chatService.getMessages(roomId, userId));
    }

    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markRoomAsRead(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(chatService.markRoomAsRead(roomId, userId));
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId,
            @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(roomId, userId, request));
    }

    @GetMapping("/events")
    public SseEmitter events(@AuthenticationPrincipal String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return chatSseService.connect(userId);
    }
}
