package com.example.communication.dto;

import java.util.List;

public record AssistantMessageResponse(
        String answer,
        String source,
        List<String> suggestions
) {
}
