package com.example.notifications.kafka;

import java.time.OffsetDateTime;

public record CourseCompletedEvent(
        String userId,
        String courseId,
        OffsetDateTime timestamp
) {
}
