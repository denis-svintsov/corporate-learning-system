package com.example.users.cabinet.dto;

public record UserCourseProgressDto(
        String courseId,
        String courseTitle,
        int completedLessons,
        int totalLessons,
        int progressPercentage
) {
}
