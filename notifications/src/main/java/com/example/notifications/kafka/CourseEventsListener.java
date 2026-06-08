package com.example.notifications.kafka;

import com.example.notifications.dto.CreateNotificationRequest;
import com.example.notifications.model.NotificationType;
import com.example.notifications.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventsListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

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
}
