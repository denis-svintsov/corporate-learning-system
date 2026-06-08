package org.example.courses.dto;

import org.example.courses.model.AssignmentRequestStatus;
import org.example.courses.model.DifficultyLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AssignmentRequestDto(
        String id,
        String userId,
        String requestedBy,
        String courseId,
        String courseTitle,
        DifficultyLevel courseDifficulty,
        Integer courseDurationMinutes,
        BigDecimal courseCompanyCost,
        String comment,
        LocalDate dueDate,
        AssignmentRequestStatus status,
        String reviewedBy,
        String reviewerComment,
        OffsetDateTime createdAt,
        OffsetDateTime reviewedAt
) {
}
