package com.example.notifications.kafka;

import com.example.notifications.dto.CreateNotificationRequest;
import com.example.notifications.model.NotificationType;
import com.example.notifications.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventsListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Value("${notifications.assignment-reviewer-ids:11111111-1111-4111-8111-111111111111,22222222-2222-4222-8222-222222222222}")
    private String assignmentReviewerIds;

    @KafkaListener(topics = KafkaTopics.COURSE_ASSIGNED)
    public void onCourseAssigned(String payload) {
        try {
            CourseAssignedEvent event = objectMapper.readValue(payload, CourseAssignedEvent.class);
            notificationService.create(new CreateNotificationRequest(
                    event.userId(),
                    NotificationType.COURSE_ASSIGNED,
                    "Назначен новый курс",
                    "Вам назначен курс. Откройте раздел назначенных курсов, чтобы начать обучение.",
                    "courses",
                    event.courseId()
            ));
        } catch (Exception ex) {
            log.warn("Failed to process course.assigned event: {}", payload, ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.LESSON_COMPLETED)
    public void onLessonCompleted(String payload) {
        try {
            LessonCompletedEvent event = objectMapper.readValue(payload, LessonCompletedEvent.class);
            notificationService.create(new CreateNotificationRequest(
                    event.userId(),
                    NotificationType.LESSON_COMPLETED,
                    "Урок завершен",
                    "Прогресс по курсу обновлен после завершения урока.",
                    "courses",
                    event.lessonId()
            ));
        } catch (Exception ex) {
            log.warn("Failed to process lesson.completed event: {}", payload, ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.COURSE_COMPLETED)
    public void onCourseCompleted(String payload) {
        try {
            CourseCompletedEvent event = objectMapper.readValue(payload, CourseCompletedEvent.class);
            notificationService.create(new CreateNotificationRequest(
                    event.userId(),
                    NotificationType.COURSE_COMPLETED,
                    "Курс завершен",
                    "Поздравляем! Курс завершен, сертификат будет доступен в разделе сертификатов.",
                    "courses",
                    event.courseId()
            ));
        } catch (Exception ex) {
            log.warn("Failed to process course.completed event: {}", payload, ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.ASSIGNMENT_REQUESTED)
    public void onAssignmentRequested(String payload) {
        try {
            AssignmentRequestedEvent event = objectMapper.readValue(payload, AssignmentRequestedEvent.class);
            reviewerIds().forEach(reviewerId -> notificationService.create(new CreateNotificationRequest(
                    reviewerId,
                    NotificationType.ASSIGNMENT_REQUESTED,
                    "Новая заявка на курс",
                    "Сотрудник отправил заявку на курс \"" + safeCourseTitle(event.courseTitle()) + "\". Проверьте раздел заявок и лимитов.",
                    "courses",
                    event.requestId()
            )));
        } catch (Exception ex) {
            log.warn("Failed to process assignment.requested event: {}", payload, ex);
        }
    }

    private List<String> reviewerIds() {
        if (assignmentReviewerIds == null || assignmentReviewerIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(assignmentReviewerIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
    }

    private String safeCourseTitle(String title) {
        return title == null || title.isBlank() ? "курс" : title;
    }
}
