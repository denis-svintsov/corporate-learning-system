package com.example.communication.repository;

import com.example.communication.model.ChatRoom;
import com.example.communication.model.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    Optional<ChatRoom> findByCourseIdAndType(String courseId, ChatRoomType type);
    List<ChatRoom> findByIdInOrderByUpdatedAtDesc(List<String> roomIds);
}
