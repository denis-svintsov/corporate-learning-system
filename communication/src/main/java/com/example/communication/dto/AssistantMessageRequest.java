package com.example.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantMessageRequest(
        @NotBlank @Size(max = 4000) String content
) {
}
