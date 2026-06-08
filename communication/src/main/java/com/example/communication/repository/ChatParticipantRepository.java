package com.example.communication.repository;

import com.example.communication.model.ChatParticipant;
import com.example.communication.model.ChatParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {
    List<ChatParticipant> findByUserId(String userId);
    List<ChatParticipant> findByRoomId(String roomId);
    boolean existsByRoomIdAndUserId(String roomId, String userId);
}
