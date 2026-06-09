package com.example.notifications.service;

import com.example.notifications.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class NotificationStreamService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ignored -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            remove(userId, emitter);
        }

        return emitter;
    }

    public void publish(NotificationDto notification) {
        List<SseEmitter> emitters = emittersByUser.get(notification.userId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .id(notification.id())
                        .data(notification));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Removing closed notification stream for user {}", notification.userId());
                remove(notification.userId(), emitter);
            }
        });
    }

    @Scheduled(fixedRate = 25000)
    public void heartbeat() {
        emittersByUser.forEach((userId, emitters) -> emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ok"));
            } catch (IOException | IllegalStateException ex) {
                remove(userId, emitter);
            }
        }));
    }

    private void remove(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }
}
