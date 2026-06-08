package com.example.communication.courses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class CoursesServiceClient {
    private static final Logger log = LoggerFactory.getLogger(CoursesServiceClient.class);

    private final HttpClient httpClient;
    private final String coursesServiceUrl;

    public CoursesServiceClient(
            @Value("${courses.service.url:http://localhost:8081}") String coursesServiceUrl
    ) {
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

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8081";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
