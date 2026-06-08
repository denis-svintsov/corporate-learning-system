package org.example.courses.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmCourseCompletionRequest(
        @NotBlank String userId,
        Boolean passed
) {
}
