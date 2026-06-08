package com.example.communication.kafka;

import com.example.communication.service.ChatService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CourseAssignedListener {

    private final ChatService chatService;

    public CourseAssignedListener(ChatService chatService) {
        this.chatService = chatService;
    }

    @KafkaListener(
            topics = KafkaTopics.COURSE_ASSIGNED,
            containerFactory = "courseAssignedKafkaListenerContainerFactory"
    )
    public void handleCourseAssigned(CourseAssignedEvent event) {
        if (event == null) return;
        chatService.ensureCourseRoomParticipant(event.courseId(), event.userId(), event.assignedBy());
    }
}
