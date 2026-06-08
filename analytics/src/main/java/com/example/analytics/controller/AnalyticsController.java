package com.example.analytics.controller;

import com.example.analytics.dto.AnalyticsDtos.CourseStatsDto;
import com.example.analytics.dto.AnalyticsDtos.AnalyticsDashboardDto;
import com.example.analytics.dto.AnalyticsDtos.DashboardOverviewDto;
import com.example.analytics.dto.AnalyticsDtos.DepartmentProgressDto;
import com.example.analytics.dto.AnalyticsDtos.GenerateReportRequest;
import com.example.analytics.dto.AnalyticsDtos.LearningReportDto;
import com.example.analytics.dto.AnalyticsDtos.NotificationEffectivenessDto;
import com.example.analytics.dto.AnalyticsDtos.UserEngagementDto;
import com.example.analytics.service.AnalyticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public AnalyticsDashboardDto dashboard(
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return analyticsService.dashboard(rolesHeader);
    }

    @GetMapping("/dashboard/overview")
    public DashboardOverviewDto overview(
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return analyticsService.overview(rolesHeader);
    }

    @GetMapping("/department-progress")
    public List<DepartmentProgressDto> departmentProgress(
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return analyticsService.departmentProgress(rolesHeader);
    }

    @GetMapping("/course-stats")
    public List<CourseStatsDto> courseStats(
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return analyticsService.courseStats(rolesHeader);
    }

    @GetMapping("/user-engagement")
    public List<UserEngagementDto> userEngagement(
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return analyticsService.userEngagement(rolesHeader);
    }

    @GetMapping("/notification-effectiveness")
    public List<NotificationEffectivenessDto> notificationEffectiveness(
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return analyticsService.notificationEffectiveness(rolesHeader);
    }

    @PostMapping("/reports/generate")
    public LearningReportDto generateReport(
            @RequestBody(required = false) GenerateReportRequest request,
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return analyticsService.generateReport(request, userId, rolesHeader);
    }

    @GetMapping("/reports")
    public List<LearningReportDto> reports(
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return analyticsService.reports(rolesHeader);
    }

    @GetMapping("/reports/{id}/download")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable String id,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        AnalyticsService.ReportFile report = analyticsService.downloadReport(id, rolesHeader);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, report.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
                .body(report.bytes());
    }
}
