package com.example.analytics.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }

    public record DashboardOverviewDto(
            long departments,
            long users,
            long activeUsers,
            long courses,
            long completedCourses,
            double averageProgress,
            double completionRate,
            OffsetDateTime generatedAt
    ) {
    }

    public record DepartmentProgressDto(
            String departmentId,
            String departmentName,
            long totalUsers,
            long activeUsers,
            double averageProgress,
            double completionRate
    ) {
    }

    public record CourseStatsDto(
            String courseId,
            String courseTitle,
            long totalEnrollments,
            long completions,
            long lessonsCompleted,
            double averageProgress,
            double completionRate
    ) {
    }

    public record UserEngagementDto(
            String userId,
            String userName,
            String departmentName,
            long assignedCourses,
            long completedLessons,
            double averageProgress,
            boolean active
    ) {
    }

    public record NotificationEffectivenessDto(
            String type,
            String channel,
            long sentCount,
            long readCount,
            double deliveryRate
    ) {
    }

    public record GenerateReportRequest(
            String reportType,
            String format
    ) {
    }

    public record LearningReportDto(
            String id,
            String reportType,
            OffsetDateTime generatedAt,
            String generatedBy,
            String format,
            Map<String, Object> data
    ) {
    }

    public record ReportPayload(
            DashboardOverviewDto overview,
            List<DepartmentProgressDto> departmentProgress,
            List<CourseStatsDto> courseStats,
            List<UserEngagementDto> userEngagement
    ) {
    }

    public record AnalyticsDashboardDto(
            DashboardOverviewDto overview,
            List<DepartmentProgressDto> departmentProgress,
            List<CourseStatsDto> courseStats,
            List<UserEngagementDto> userEngagement,
            List<NotificationEffectivenessDto> notificationEffectiveness,
            List<LearningReportDto> reports
    ) {
    }
}
