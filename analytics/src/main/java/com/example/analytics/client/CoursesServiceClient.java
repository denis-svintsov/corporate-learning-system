package com.example.analytics.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
public class CoursesServiceClient {

    private final RestClient restClient;

    public CoursesServiceClient(RestClient.Builder builder, @Value("${courses.service.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<CourseDto> getCourses() {
        PageDto<CourseDto> page = restClient.get()
                .uri("/courses?size=100&status=ACTIVE")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return page == null || page.content() == null ? List.of() : page.content();
    }

    public ProgressSummaryDto getUserProgress(String userId, String rolesHeader) {
        ProgressSummaryDto progress = restClient.get()
                .uri("/progress/users/{userId}", userId)
                .header("X-Roles", rolesHeader == null ? "" : rolesHeader)
                .retrieve()
                .body(ProgressSummaryDto.class);
        return progress == null ? new ProgressSummaryDto(userId, List.of()) : progress;
    }

    public List<ProgressSummaryDto> getUsersProgress(List<String> userIds, String rolesHeader) {
        List<ProgressSummaryDto> progress = restClient.post()
                .uri("/progress/users/bulk")
                .header("X-Roles", rolesHeader == null ? "" : rolesHeader)
                .body(userIds == null ? List.of() : userIds)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return progress == null ? List.of() : progress;
    }

    public record PageDto<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int size,
            int number
    ) {
    }

    public record CourseDto(
            String id,
            String title,
            String description,
            String categoryId,
            String difficulty,
            Integer durationMinutes,
            String status,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record ProgressSummaryDto(
            String userId,
            List<UserCourseProgressDto> courses
    ) {
    }

    public record UserCourseProgressDto(
            String courseId,
            String courseTitle,
            int completedLessons,
            int totalLessons,
            int progressPercentage
    ) {
    }
}
