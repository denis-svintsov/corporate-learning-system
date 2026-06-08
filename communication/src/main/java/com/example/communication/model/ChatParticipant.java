package com.example.communication.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@IdClass(ChatParticipantId.class)
@Table(name = "chat_participant", schema = "communication")
public class ChatParticipant {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "room_id")
    private String roomId;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ChatParticipantRole role;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
        if (role == null) {
            role = ChatParticipantRole.PARTICIPANT;
        }
    }
}
