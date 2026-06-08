package com.example.communication.model;

import java.io.Serializable;
import java.util.Objects;

public class ChatParticipantId implements Serializable {
    private String userId;
    private String roomId;

    public ChatParticipantId() {}

    public ChatParticipantId(String userId, String roomId) {
        this.userId = userId;
        this.roomId = roomId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatParticipantId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(roomId, that.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roomId);
    }
}
