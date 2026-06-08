package com.example.communication.dto;

import com.example.communication.model.ChatRoomType;

import java.time.OffsetDateTime;

public record ChatRoomDto(
        String id,
        String courseId,
        String name,
        ChatRoomType type,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
