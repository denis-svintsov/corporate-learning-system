package com.example.users.cabinet.dto;

import java.time.OffsetDateTime;

public record LearningHistoryDto(
        String id,
        String action,
        OffsetDateTime timestamp,
        String details
) {
}
