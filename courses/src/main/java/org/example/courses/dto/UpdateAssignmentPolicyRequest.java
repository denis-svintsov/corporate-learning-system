package org.example.courses.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateAssignmentPolicyRequest(
        @NotNull @Min(1) @Max(100) Integer maxCoursesPerQuarter
) {
}
