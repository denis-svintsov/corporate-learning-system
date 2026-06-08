package org.example.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.example.courses.model.DifficultyLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record CreateCourseRequest(
        @NotBlank String title,
        String description,
        String categoryId,
        @NotNull DifficultyLevel difficulty,
        @PositiveOrZero Integer durationMinutes,
        Set<String> tagIds,
        Set<String> allowedRoles,
        Set<String> allowedDepartmentIds,
        Set<String> specializations,
        String instructions,
        String aggregatorUrl,
        String coverUrl,
        @PositiveOrZero BigDecimal companyCost,
        String partnerName,
        String partnerLocation,
        LocalDate startDate,
        LocalDate endDate
) {
}
