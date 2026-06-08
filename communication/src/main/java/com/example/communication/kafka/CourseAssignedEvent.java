package com.example.communication.kafka;

import java.time.OffsetDateTime;

public record CourseAssignedEvent(
        String userId,
        String courseId,
        String assignedBy,
        OffsetDateTime timestamp
) {
}
