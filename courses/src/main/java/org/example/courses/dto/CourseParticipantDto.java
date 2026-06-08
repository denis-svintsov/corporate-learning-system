package org.example.courses.dto;

import org.example.courses.model.AssignmentStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CourseParticipantDto(
        String userId,
        String assignmentId,
        AssignmentStatus assignmentStatus,
        LocalDate dueDate,
        OffsetDateTime assignedAt
) {
}
