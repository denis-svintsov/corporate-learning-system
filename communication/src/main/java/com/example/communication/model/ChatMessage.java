package com.example.communication.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chat_message", schema = "communication")
public class ChatMessage {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "\"timestamp\"")
    private OffsetDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(name = "attachments_json", columnDefinition = "text")
    private String attachmentsJson;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = OffsetDateTime.now();
        }
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }
    }
}
