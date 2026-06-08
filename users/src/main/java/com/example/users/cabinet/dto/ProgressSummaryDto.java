package com.example.users.cabinet.dto;

import java.util.List;

public record ProgressSummaryDto(
        String userId,
        List<UserCourseProgressDto> courses
) {
}
