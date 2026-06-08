package com.example.notifications.kafka;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    public static final String COURSE_ASSIGNED = "course.assigned";
    public static final String LESSON_COMPLETED = "lesson.completed";
    public static final String COURSE_COMPLETED = "course.completed";
    public static final String ASSIGNMENT_REQUESTED = "assignment.requested";
}
