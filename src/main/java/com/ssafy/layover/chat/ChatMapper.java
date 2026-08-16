package com.ssafy.layover.chat;

import com.ssafy.layover.chat.dto.ChatMessageRaw;
import com.ssafy.layover.chat.dto.ChatRoomRaw;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatMapper {

    String findRoomByPair(@Param("userLowId") String userLowId, @Param("userHighId") String userHighId);

    void insertRoom(@Param("id") String id,
                    @Param("userLowId") String userLowId,
                    @Param("userHighId") String userHighId);

    void insertParticipant(@Param("roomId") String roomId, @Param("userId") String userId);

    boolean isParticipant(@Param("roomId") String roomId, @Param("userId") String userId);

    List<String> findParticipantUserIds(@Param("roomId") String roomId);

    List<ChatRoomRaw> findRoomsByUserId(@Param("userId") String userId);

    List<ChatMessageRaw> findMessages(@Param("roomId") String roomId);

    ChatMessageRaw findMessageById(@Param("id") String id);

    void insertMessage(@Param("id") String id,
                       @Param("roomId") String roomId,
                       @Param("senderId") String senderId,
                       @Param("type") String type,
                       @Param("encryptedContent") String encryptedContent,
                       @Param("iv") String iv);

    void updateRoomTimestamp(@Param("roomId") String roomId);

    void updateLastReadAt(@Param("roomId") String roomId,
                          @Param("userId") String userId,
                          @Param("lastReadAt") LocalDateTime lastReadAt);
}
