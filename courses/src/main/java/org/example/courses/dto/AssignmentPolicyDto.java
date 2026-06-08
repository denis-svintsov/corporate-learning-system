package org.example.courses.dto;

import java.time.OffsetDateTime;

public record AssignmentPolicyDto(
        int maxCoursesPerQuarter,
        OffsetDateTime updatedAt
) {
}
