package com.example.communication.dto;

import com.example.communication.model.ChatParticipantRole;

import java.time.OffsetDateTime;

public record ChatParticipantDto(
        String userId,
        String roomId,
        OffsetDateTime joinedAt,
        ChatParticipantRole role,
        boolean online
) {
}
