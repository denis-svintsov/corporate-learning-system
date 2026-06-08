package org.example.courses.dto;

import org.example.courses.model.AssignmentStatus;
import org.example.courses.model.CourseStatus;
import org.example.courses.model.DifficultyLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CourseAssignmentDto(
        String id,
        String userId,
        String courseId,
        String courseTitle,
        String courseDescription,
        String courseCategoryId,
        DifficultyLevel courseDifficulty,
        Integer courseDurationMinutes,
        CourseStatus courseStatus,
        String courseCoverUrl,
        String courseAggregatorUrl,
        String courseInstructions,
        java.util.Set<String> courseSpecializations,
        BigDecimal courseCompanyCost,
        LocalDate courseStartDate,
        LocalDate courseEndDate,
        String assignedBy,
        LocalDate dueDate,
        AssignmentStatus status,
        OffsetDateTime createdAt
) {
}
