package com.ssafy.layover.chat;

import com.ssafy.layover.chat.dto.*;
import com.ssafy.layover.common.crypto.CryptoService;
import com.ssafy.layover.common.crypto.EncryptedValue;
import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;
    private final ChatSseService chatSseService;
    private final ChatRedisService chatRedisService;

    @Transactional
    public ApiResponse<ChatRoomResponse> createOrGetRoom(String currentUserId, String otherUserId) {
        if (otherUserId == null || otherUserId.isBlank()) {
            return ApiResponse.fail("대화 상대를 선택해주세요.");
        }
        if (currentUserId.equals(otherUserId)) {
            return ApiResponse.fail("본인과는 채팅할 수 없습니다.");
        }
        if (!userRepository.existsById(otherUserId)) {
            return ApiResponse.fail("존재하지 않는 사용자입니다.");
        }

        String[] pair = sortedPair(currentUserId, otherUserId);
        String roomId = chatMapper.findRoomByPair(pair[0], pair[1]);
        if (roomId == null) {
            roomId = UUID.randomUUID().toString();
            chatMapper.insertRoom(roomId, pair[0], pair[1]);
            chatMapper.insertParticipant(roomId, currentUserId);
            chatMapper.insertParticipant(roomId, otherUserId);
        }

        return ApiResponse.success(findRoomResponse(currentUserId, roomId, otherUserId));
    }

    public List<ChatRoomResponse> getRooms(String userId) {
        return chatMapper.findRoomsByUserId(userId).stream()
                .map(this::toRoomResponse)
                .sorted(Comparator.comparing(ChatRoomResponse::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public ApiResponse<List<ChatMessageResponse>> getMessages(String roomId, String userId) {
        if (!chatMapper.isParticipant(roomId, userId)) {
            return ApiResponse.fail("채팅방 접근 권한이 없습니다.");
        }
        chatMapper.updateLastReadAt(roomId, userId, LocalDateTime.now());
        return ApiResponse.success(chatMapper.findMessages(roomId).stream()
                .map(this::toMessageResponse)
                .toList());
    }

    @Transactional
    public ApiResponse<Void> markRoomAsRead(String roomId, String userId) {
        if (!chatMapper.isParticipant(roomId, userId)) {
            return ApiResponse.fail("채팅방 접근 권한이 없습니다.");
        }
        chatMapper.updateLastReadAt(roomId, userId, LocalDateTime.now());
        return ApiResponse.success("읽음 처리되었습니다.", null);
    }

    @Transactional
    public ApiResponse<ChatMessageResponse> sendMessage(String roomId, String userId, ChatMessageRequest request) {
        if (!chatMapper.isParticipant(roomId, userId)) {
            return ApiResponse.fail("채팅방 접근 권한이 없습니다.");
        }
        String type = normalizeType(request.getType());
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            return ApiResponse.fail("메시지 내용을 입력해주세요.");
        }

        EncryptedValue encrypted = cryptoService.encrypt(content);
        String messageId = UUID.randomUUID().toString();
        chatMapper.insertMessage(messageId, roomId, userId, type, encrypted.getCipherText(), encrypted.getIv());
        chatMapper.updateRoomTimestamp(roomId);

        ChatMessageResponse response = toMessageResponse(chatMapper.findMessageById(messageId));
        ChatEventResponse event = new ChatEventResponse(roomId, response);
        chatRedisService.publishMessage(event);
        chatMapper.findParticipantUserIds(roomId).forEach(participantId -> chatSseService.send(participantId, event));
        return ApiResponse.success(response);
    }

    private ChatRoomResponse findRoomResponse(String currentUserId, String roomId, String otherUserId) {
        User other = userRepository.findById(otherUserId).orElseThrow();
        return new ChatRoomResponse(
                roomId,
                other.getId(),
                other.getUsername(),
                other.getProfileImage(),
                null,
                null,
                null,
                0,
                null
        );
    }

    private ChatRoomResponse toRoomResponse(ChatRoomRaw raw) {
        String lastMessage = null;
        if (raw.getLastMessageEncryptedContent() != null) {
            String decrypted = cryptoService.decrypt(raw.getLastMessageEncryptedContent(), raw.getLastMessageIv());
            if ("IMAGE".equals(raw.getLastMessageType())) {
                lastMessage = "사진을 보냈습니다.";
            } else if ("COURSE".equals(raw.getLastMessageType())) {
                lastMessage = "코스를 공유했습니다.";
            } else {
                lastMessage = decrypted;
            }
        }
        return new ChatRoomResponse(
                raw.getRoomId(),
                raw.getOtherUserId(),
                raw.getOtherUsername(),
                raw.getOtherProfileImage(),
                lastMessage,
                raw.getLastMessageType(),
                raw.getLastMessageSenderId(),
                raw.getUnreadCount(),
                raw.getUpdatedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessageRaw raw) {
        return new ChatMessageResponse(
                raw.getId(),
                raw.getRoomId(),
                raw.getSenderId(),
                raw.getSenderUsername(),
                raw.getType(),
                cryptoService.decrypt(raw.getEncryptedContent(), raw.getIv()),
                raw.getCreatedAt()
        );
    }

    private String normalizeType(String type) {
        if ("IMAGE".equalsIgnoreCase(type)) {
            return "IMAGE";
        }
        if ("COURSE".equalsIgnoreCase(type)) {
            return "COURSE";
        }
        return "TEXT";
    }

    private String[] sortedPair(String first, String second) {
        return first.compareTo(second) <= 0
                ? new String[]{first, second}
                : new String[]{second, first};
    }
}
