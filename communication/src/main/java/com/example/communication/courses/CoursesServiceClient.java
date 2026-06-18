package com.example.communication.courses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class CoursesServiceClient {
    private static final Logger log = LoggerFactory.getLogger(CoursesServiceClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String coursesServiceUrl;

    public CoursesServiceClient(
            @Value("${courses.service.url:http://localhost:8081}") String coursesServiceUrl
    ) {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.coursesServiceUrl = trimTrailingSlash(coursesServiceUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public boolean hasAssignment(String userId, String courseId) {
        if (userId == null || userId.isBlank() || courseId == null || courseId.isBlank()) {
            return false;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(coursesServiceUrl + "/internal/courses/" + courseId + "/assignments/" + userId + "/exists"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Boolean.parseBoolean(response.body().trim());
            }
            log.warn("Courses service returned status={} while checking assignment userId={} courseId={}",
                    response.statusCode(), userId, courseId);
            return false;
        } catch (IOException ex) {
            log.warn("Courses service is unavailable while checking assignment userId={} courseId={}", userId, courseId, ex);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Assignment check was interrupted userId={} courseId={}", userId, courseId, ex);
            return false;
        }
    }

    public List<AssignedCourse> getAssignedCourses(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(coursesServiceUrl + "/users/" + userId + "/assigned-courses"))
                .timeout(Duration.ofMillis(1200))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Courses service returned status={} while loading assigned courses userId={}",
                        response.statusCode(), userId);
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                return List.of();
            }

            List<AssignedCourse> result = new ArrayList<>();
            for (JsonNode node : root) {
                result.add(new AssignedCourse(
                        text(node, "courseId"),
                        text(node, "courseTitle"),
                        text(node, "courseDifficulty"),
                        integer(node, "courseDurationMinutes"),
                        text(node, "status"),
                        date(node, "courseStartDate"),
                        date(node, "courseEndDate"),
                        date(node, "dueDate"),
                        decimal(node, "courseCompanyCost")
                ));
            }
            return result;
        } catch (IOException ex) {
            log.warn("Courses service is unavailable while loading assigned courses userId={}", userId, ex);
            return List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Assigned courses loading was interrupted userId={}", userId, ex);
            return List.of();
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8081";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private LocalDate date(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : LocalDate.parse(value);
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : new BigDecimal(value);
    }

    public record AssignedCourse(
            String courseId,
            String courseTitle,
            String courseDifficulty,
            Integer courseDurationMinutes,
            String status,
            LocalDate courseStartDate,
            LocalDate courseEndDate,
            LocalDate dueDate,
            BigDecimal courseCompanyCost
    ) {
    }
}
