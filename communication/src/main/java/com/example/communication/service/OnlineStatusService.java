package com.example.communication.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OnlineStatusService {

    private static final String KEY_PREFIX = "chat:online:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redisTemplate;

    public OnlineStatusService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void markOnline(String userId) {
        if (userId == null || userId.isBlank()) return;
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", TTL);
    }

    public void markOffline(String userId) {
        if (userId == null || userId.isBlank()) return;
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    public boolean isOnline(String userId) {
        if (userId == null || userId.isBlank()) return false;
        Boolean present = redisTemplate.hasKey(KEY_PREFIX + userId);
        return Boolean.TRUE.equals(present);
    }
}
