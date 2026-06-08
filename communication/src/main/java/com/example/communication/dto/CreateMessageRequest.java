package com.example.communication.dto;

import com.example.communication.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateMessageRequest(
        @NotBlank @Size(max = 4000) String content,
        MessageType messageType,
        @Size(max = 10) List<@Size(max = 2048) String> attachments
) {
}
