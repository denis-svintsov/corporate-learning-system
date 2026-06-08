package com.example.notifications.dto;

import com.example.notifications.model.NotificationType;
import jakarta.validation.constraints.NotBlank;

public record CreateNotificationRequest(
        @NotBlank String userId,
        NotificationType type,
        @NotBlank String title,
        @NotBlank String message,
        String sourceService,
        String sourceId
) {
}
