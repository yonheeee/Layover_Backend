package com.ssafy.layover.chat;

import com.ssafy.layover.chat.dto.ChatEventResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatSseService {

    private final ConcurrentHashMap<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(String userId) {
        SseEmitter emitter = new SseEmitter(60L * 60L * 1000L);
        emitters.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    public void send(String userId, ChatEventResponse event) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(event));
            } catch (IOException | IllegalStateException e) {
                remove(userId, emitter);
            }
        }
    }

    private void remove(String userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }
}
