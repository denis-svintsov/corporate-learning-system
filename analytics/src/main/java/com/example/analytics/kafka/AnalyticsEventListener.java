package com.example.analytics.kafka;

import com.example.analytics.model.CourseStat;
import com.example.analytics.model.UserActivity;
import com.example.analytics.repository.CourseStatRepository;
import com.example.analytics.repository.UserActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public class AnalyticsEventListener {

    private final CourseStatRepository courseStatRepository;
    private final UserActivityRepository userActivityRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsEventListener(
            CourseStatRepository courseStatRepository,
            UserActivityRepository userActivityRepository,
            ObjectMapper objectMapper
    ) {
        this.courseStatRepository = courseStatRepository;
        this.userActivityRepository = userActivityRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "course.assigned")
    public void onCourseAssigned(String payload) {
        JsonNode event = read(payload);
        String courseId = text(event, "courseId");
        String userId = text(event, "userId");
        LocalDate date = eventDate(event);
        if (courseId != null) {
            CourseStat stat = courseStatRepository.findById(courseId).orElseGet(() -> new CourseStat(courseId));
            stat.addAssignment();
            courseStatRepository.save(stat);
        }
        if (userId != null) {
            UserActivity activity = userActivityRepository.findByUserIdAndActivityDate(userId, date)
                    .orElseGet(() -> new UserActivity(userId, date));
            activity.addAssignmentReceived();
            userActivityRepository.save(activity);
        }
    }

    @Transactional
    @KafkaListener(topics = "lesson.completed")
    public void onLessonCompleted(String payload) {
        JsonNode event = read(payload);
        String courseId = text(event, "courseId");
        String userId = text(event, "userId");
        LocalDate date = eventDate(event);
        if (courseId != null) {
            CourseStat stat = courseStatRepository.findById(courseId).orElseGet(() -> new CourseStat(courseId));
            stat.addLessonCompleted();
            courseStatRepository.save(stat);
        }
        if (userId != null) {
            UserActivity activity = userActivityRepository.findByUserIdAndActivityDate(userId, date)
                    .orElseGet(() -> new UserActivity(userId, date));
            activity.addLessonCompleted();
            userActivityRepository.save(activity);
        }
    }

    @Transactional
    @KafkaListener(topics = "course.completed")
    public void onCourseCompleted(String payload) {
        JsonNode event = read(payload);
        String courseId = text(event, "courseId");
        String userId = text(event, "userId");
        LocalDate date = eventDate(event);
        if (courseId != null) {
            CourseStat stat = courseStatRepository.findById(courseId).orElseGet(() -> new CourseStat(courseId));
            stat.addCompletion();
            courseStatRepository.save(stat);
        }
        if (userId != null) {
            UserActivity activity = userActivityRepository.findByUserIdAndActivityDate(userId, date)
                    .orElseGet(() -> new UserActivity(userId, date));
            activity.addCourseCompleted();
            userActivityRepository.save(activity);
        }
    }

    private JsonNode read(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid analytics event payload", ex);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private LocalDate eventDate(JsonNode node) {
        String timestamp = text(node, "timestamp");
        if (timestamp == null) {
            return LocalDate.now();
        }
        return OffsetDateTime.parse(timestamp).toLocalDate();
    }
}
