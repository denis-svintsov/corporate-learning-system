package org.example.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.courses.model.AttendanceStatus;

import java.time.LocalDate;

public record MarkAttendanceRequest(
        @NotBlank String userId,
        @NotNull LocalDate attendanceDate,
        @NotNull AttendanceStatus status,
        String comment
) {
}
