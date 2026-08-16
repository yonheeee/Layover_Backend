package com.ssafy.layover.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.layover.chat.dto.ChatEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatRedisService {

    private static final String CHANNEL = "layover:chat:messages";
    private static final String LAST_MESSAGE_KEY_PREFIX = "chat:last:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishMessage(ChatEventResponse event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL, payload);
            redisTemplate.opsForValue().set(
                    LAST_MESSAGE_KEY_PREFIX + event.getRoomId(),
                    payload,
                    Duration.ofDays(1)
            );
        } catch (Exception ignored) {
            // Redis는 실시간/캐시 보조 역할입니다. 실패해도 DB 저장 흐름은 유지합니다.
        }
    }
}
