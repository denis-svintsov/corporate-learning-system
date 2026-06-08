package com.example.notifications.kafka;

import java.time.OffsetDateTime;

public record CourseAssignedEvent(
        String userId,
        String courseId,
        String assignedBy,
        OffsetDateTime timestamp
) {
}
