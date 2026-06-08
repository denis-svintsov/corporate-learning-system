package org.example.courses.service;

import org.example.courses.dto.CourseDto;
import org.example.courses.model.Course;
import org.example.courses.util.CsvUtil;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CourseMapper {

    public CourseDto toDto(Course course) {
        Set<String> tagIds = course.getTags() == null ? Set.of() : course.getTags().stream()
                .map(t -> t.getId())
                .collect(Collectors.toSet());

        return new CourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCategory() != null ? course.getCategory().getId() : null,
                course.getDifficulty(),
                course.getDurationMinutes(),
                course.getStatus(),
                tagIds,
                CsvUtil.splitToSet(course.getAllowedRolesCsv()),
                CsvUtil.splitToSet(course.getAllowedDepartmentIdsCsv()),
                CsvUtil.splitToSet(course.getSpecializationsCsv()),
                course.getInstructions(),
                course.getAggregatorUrl(),
                course.getCoverUrl(),
                course.getCompanyCost(),
                course.getPartnerName(),
                course.getPartnerLocation(),
                course.getStartDate(),
                course.getEndDate()
        );
    }
}
