package org.example.courses.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCourseAssigned(CourseAssignedEvent event) {
        kafkaTemplate.send(KafkaTopics.COURSE_ASSIGNED, event.userId(), event);
    }

    public void publishLessonCompleted(LessonCompletedEvent event) {
        kafkaTemplate.send(KafkaTopics.LESSON_COMPLETED, event.userId(), event);
    }

    public void publishCourseCompleted(CourseCompletedEvent event) {
        kafkaTemplate.send(KafkaTopics.COURSE_COMPLETED, event.userId(), event);
    }

    public void publishAssignmentRequested(AssignmentRequestedEvent event) {
        kafkaTemplate.send(KafkaTopics.ASSIGNMENT_REQUESTED, event.requestId(), event);
    }
}
