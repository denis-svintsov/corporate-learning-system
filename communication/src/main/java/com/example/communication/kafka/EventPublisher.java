package com.example.communication.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishNewMessage(NewMessageEvent event) {
        kafkaTemplate.send(KafkaTopics.CHAT_NEW_MESSAGE, event.roomId(), event);
    }
}
