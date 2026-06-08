package org.example.courses.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssignmentRequestDecisionRequest(
        @Size(max = 1000) String reviewerComment,
        LocalDate dueDate
) {
}

