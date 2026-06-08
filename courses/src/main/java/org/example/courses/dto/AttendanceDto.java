package org.example.courses.dto;

import org.example.courses.model.AttendanceStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AttendanceDto(
        String id,
        String courseId,
        String userId,
        LocalDate attendanceDate,
        AttendanceStatus status,
        String comment,
        String markedBy,
        OffsetDateTime updatedAt
) {
}
