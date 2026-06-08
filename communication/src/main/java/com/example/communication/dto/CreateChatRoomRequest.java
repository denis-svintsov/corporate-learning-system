package com.example.communication.dto;

import com.example.communication.model.ChatRoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateChatRoomRequest(
        String courseId,
        @NotBlank String name,
        @NotNull ChatRoomType type,
        List<String> participants
) {
}
