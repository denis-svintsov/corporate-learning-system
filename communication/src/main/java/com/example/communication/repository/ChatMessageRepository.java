package com.example.communication.repository;

import com.example.communication.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByRoomIdOrderByTimestampDesc(String roomId, Pageable pageable);
    List<ChatMessage> findByRoomIdAndTimestampLessThanOrderByTimestampDesc(String roomId, OffsetDateTime before, Pageable pageable);
}
