package com.example.notifications.kafka;

import java.time.OffsetDateTime;

public record AssignmentRequestedEvent(
        String requestId,
        String userId,
        String courseId,
        String courseTitle,
        OffsetDateTime requestedAt
) {
}
