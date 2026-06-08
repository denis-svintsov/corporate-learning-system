package com.example.users.cabinet.dto;

import java.util.List;

public record UserCabinetDto(
        ProgressSummaryDto progress,
        List<LearningHistoryDto> history
) {
}
