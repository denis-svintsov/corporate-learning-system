package org.example.courses.dto;

import org.example.courses.model.CourseStatus;
import org.example.courses.model.DifficultyLevel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record CourseDto(
        String id,
        String title,
        String description,
        String categoryId,
        DifficultyLevel difficulty,
        Integer durationMinutes,
        CourseStatus status,
        Set<String> tagIds,
        Set<String> allowedRoles,
        Set<String> allowedDepartmentIds,
        Set<String> specializations,
        String instructions,
        String aggregatorUrl,
        String coverUrl,
        BigDecimal companyCost,
        String partnerName,
        String partnerLocation,
        LocalDate startDate,
        LocalDate endDate
) implements Serializable {
}
