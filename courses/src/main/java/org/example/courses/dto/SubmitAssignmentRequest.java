package org.example.courses.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SubmitAssignmentRequest(
        @NotBlank String userId,
        @NotBlank String courseId,
        @NotBlank String requestedBy,
        @Size(max = 1000) String comment,
        @FutureOrPresent LocalDate dueDate
) {
}

