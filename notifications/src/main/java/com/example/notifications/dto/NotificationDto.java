package com.example.notifications.dto;

import com.example.notifications.model.NotificationChannel;
import com.example.notifications.model.NotificationStatus;
import com.example.notifications.model.NotificationType;

import java.time.OffsetDateTime;

public record NotificationDto(
        String id,
        String userId,
        NotificationType type,
        NotificationChannel channel,
        NotificationStatus status,
        String title,
        String message,
        String sourceService,
        String sourceId,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {
}
