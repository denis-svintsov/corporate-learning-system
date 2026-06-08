package com.example.communication.dto;

import com.example.communication.model.MessageType;

import java.time.OffsetDateTime;
import java.util.List;

public record MessageDto(
        String id,
        String roomId,
        String userId,
        String content,
        OffsetDateTime timestamp,
        MessageType messageType,
        List<String> attachments
) {
}
