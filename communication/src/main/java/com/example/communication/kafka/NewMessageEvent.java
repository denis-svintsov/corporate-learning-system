package com.example.communication.kafka;

import com.example.communication.model.MessageType;

import java.time.OffsetDateTime;

public record NewMessageEvent(
        String messageId,
        String roomId,
        String userId,
        String content,
        MessageType messageType,
        OffsetDateTime timestamp
) {
}
