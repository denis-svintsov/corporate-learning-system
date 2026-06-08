package com.example.analytics.service;

import com.example.analytics.client.CoursesServiceClient;
import com.example.analytics.client.CoursesServiceClient.ProgressSummaryDto;
import com.example.analytics.client.CoursesServiceClient.UserCourseProgressDto;
import com.example.analytics.client.NotificationsServiceClient;
import com.example.analytics.client.UsersServiceClient;
import com.example.analytics.client.UsersServiceClient.DepartmentDto;
import com.example.analytics.client.UsersServiceClient.UserProfileDto;
import com.example.analytics.dto.AnalyticsDtos.CourseStatsDto;
import com.example.analytics.dto.AnalyticsDtos.AnalyticsDashboardDto;
import com.example.analytics.dto.AnalyticsDtos.DashboardOverviewDto;
import com.example.analytics.dto.AnalyticsDtos.DepartmentProgressDto;
import com.example.analytics.dto.AnalyticsDtos.GenerateReportRequest;
import com.example.analytics.dto.AnalyticsDtos.LearningReportDto;
import com.example.analytics.dto.AnalyticsDtos.NotificationEffectivenessDto;
import com.example.analytics.dto.AnalyticsDtos.ReportPayload;
import com.example.analytics.dto.AnalyticsDtos.UserEngagementDto;
import com.example.analytics.model.CourseStat;
import com.example.analytics.model.LearningReport;
import com.example.analytics.repository.CourseStatRepository;
import com.example.analytics.repository.LearningReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final Set<String> ANALYTICS_ROLES = Set.of("ADMIN", "HR", "MANAGER", "TECHNOLOG");

    private final UsersServiceClient usersClient;
    private final CoursesServiceClient coursesClient;
    private final NotificationsServiceClient notificationsClient;
    private final CourseStatRepository courseStatRepository;
    private final LearningReportRepository learningReportRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsService(
            UsersServiceClient usersClient,
            CoursesServiceClient coursesClient,
            NotificationsServiceClient notificationsClient,
            CourseStatRepository courseStatRepository,
            LearningReportRepository learningReportRepository,
            ObjectMapper objectMapper
    ) {
        this.usersClient = usersClient;
        this.coursesClient = coursesClient;
        this.notificationsClient = notificationsClient;
        this.courseStatRepository = courseStatRepository;
        this.learningReportRepository = learningReportRepository;
        this.objectMapper = objectMapper;
    }

    public DashboardOverviewDto overview(String rolesHeader) {
        AnalyticsSnapshot snapshot = snapshot(rolesHeader);
        return buildOverview(snapshot);
    }

    public AnalyticsDashboardDto dashboard(String rolesHeader) {
        AnalyticsSnapshot snapshot = snapshot(rolesHeader);
        return new AnalyticsDashboardDto(
                buildOverview(snapshot),
                snapshot.departmentProgress(),
                snapshot.courseStats(),
                snapshot.userEngagement(),
                notificationEffectiveness(rolesHeader),
                reports(rolesHeader)
        );
    }

    public List<DepartmentProgressDto> departmentProgress(String rolesHeader) {
        return snapshot(rolesHeader).departmentProgress();
    }

    public List<CourseStatsDto> courseStats(String rolesHeader) {
        return snapshot(rolesHeader).courseStats();
    }

    public List<UserEngagementDto> userEngagement(String rolesHeader) {
        return snapshot(rolesHeader).userEngagement();
    }

    public List<NotificationEffectivenessDto> notificationEffectiveness(String rolesHeader) {
        requireAnalyticsRole(rolesHeader);
        return notificationsClient.deliveryStats().stream()
                .map(stat -> new NotificationEffectivenessDto(
                        stat.type(),
                        stat.channel(),
                        stat.sentCount(),
                        stat.readCount(),
                        stat.deliveryRate()
                ))
                .toList();
    }

    @Transactional
    public LearningReportDto generateReport(GenerateReportRequest request, String userId, String rolesHeader) {
        AnalyticsSnapshot snapshot = snapshot(rolesHeader);
        ReportPayload payload = new ReportPayload(
                buildOverview(snapshot),
                snapshot.departmentProgress(),
                snapshot.courseStats(),
                snapshot.userEngagement()
        );
        String reportType = normalize(request == null ? null : request.reportType(), "dashboard-overview");
        String format = normalize(request == null ? null : request.format(), "json");
        try {
            String json = objectMapper.writeValueAsString(payload);
            LearningReport report = learningReportRepository.save(new LearningReport(reportType, userId, format, json));
            return toDto(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize analytics report", ex);
        }
    }

    public List<LearningReportDto> reports(String rolesHeader) {
        requireAnalyticsRole(rolesHeader);
        return learningReportRepository.findAll().stream()
                .sorted(Comparator.comparing(LearningReport::getGeneratedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    private AnalyticsSnapshot snapshot(String rolesHeader) {
        requireAnalyticsRole(rolesHeader);

        long startedAt = System.currentTimeMillis();
        List<DepartmentDto> departments = usersClient.getDepartments();
        List<UserWithDepartment> users = new ArrayList<>();
        for (DepartmentDto department : departments) {
            usersClient.getDepartmentUsers(department.departmentId()).stream()
                    .map(user -> new UserWithDepartment(user, department))
                    .forEach(users::add);
        }
        log.info("analytics snapshot loaded users departments={} users={} durationMs={}",
                departments.size(), users.size(), System.currentTimeMillis() - startedAt);

        long progressStartedAt = System.currentTimeMillis();
        Map<String, ProgressSummaryDto> progressByUser = coursesClient.getUsersProgress(
                        users.stream().map(user -> user.user().id()).toList(),
                        rolesHeader
                ).stream()
                .collect(Collectors.toMap(ProgressSummaryDto::userId, Function.identity(), (left, right) -> left));
        log.info("analytics snapshot loaded progress users={} durationMs={}",
                progressByUser.size(), System.currentTimeMillis() - progressStartedAt);

        List<DepartmentProgressDto> departmentProgress = departments.stream()
                .map(department -> buildDepartmentProgress(department, users, progressByUser))
                .toList();

        List<CourseStatsDto> courseStats = buildCourseStats(progressByUser);
        List<UserEngagementDto> engagement = users.stream()
                .map(user -> buildUserEngagement(user, progressByUser.get(user.user().id())))
                .sorted(Comparator.comparing(UserEngagementDto::averageProgress).reversed())
                .toList();

        AnalyticsSnapshot snapshot = new AnalyticsSnapshot(departments, users, departmentProgress, courseStats, engagement);
        log.info("analytics snapshot built courses={} departments={} engagement={} durationMs={}",
                courseStats.size(), departmentProgress.size(), engagement.size(), System.currentTimeMillis() - startedAt);
        return snapshot;
    }

    private DepartmentProgressDto buildDepartmentProgress(
            DepartmentDto department,
            List<UserWithDepartment> allUsers,
            Map<String, ProgressSummaryDto> progressByUser
    ) {
        List<UserWithDepartment> departmentUsers = allUsers.stream()
                .filter(user -> department.departmentId().equals(user.department().departmentId()))
                .toList();

        List<ProgressSummaryDto> summaries = departmentUsers.stream()
                .map(user -> progressByUser.get(user.user().id()))
                .toList();

        return new DepartmentProgressDto(
                department.departmentId(),
                department.name(),
                departmentUsers.size(),
                summaries.stream().filter(summary -> !safeCourses(summary).isEmpty()).count(),
                averageProgress(summaries),
                completionRate(summaries)
        );
    }

    private List<CourseStatsDto> buildCourseStats(Map<String, ProgressSummaryDto> progressByUser) {
        Map<String, CourseAccumulator> byCourse = new HashMap<>();

        for (ProgressSummaryDto summary : progressByUser.values()) {
            for (UserCourseProgressDto course : safeCourses(summary)) {
                byCourse.computeIfAbsent(course.courseId(), id -> new CourseAccumulator(course.courseId(), course.courseTitle()))
                        .add(course);
            }
        }

        Map<String, CourseStat> kafkaStats = courseStatRepository.findAll().stream()
                .collect(Collectors.toMap(CourseStat::getCourseId, Function.identity()));

        return byCourse.values().stream()
                .map(accumulator -> {
                    CourseStat stat = kafkaStats.get(accumulator.courseId);
                    long lessonsCompleted = accumulator.lessonsCompleted + (stat == null ? 0 : stat.getLessonsCompleted());
                    long completions = Math.max(accumulator.completions, stat == null ? 0 : stat.getCompletions());
                    return new CourseStatsDto(
                            accumulator.courseId,
                            accumulator.courseTitle,
                            Math.max(accumulator.enrollments, stat == null ? 0 : stat.getAssignments()),
                            completions,
                            lessonsCompleted,
                            accumulator.averageProgress(),
                            percent(completions, accumulator.enrollments)
                    );
                })
                .sorted(Comparator.comparing(CourseStatsDto::totalEnrollments).reversed())
                .toList();
    }

    private UserEngagementDto buildUserEngagement(UserWithDepartment user, ProgressSummaryDto summary) {
        List<UserCourseProgressDto> courses = safeCourses(summary);
        long completedLessons = courses.stream().mapToLong(UserCourseProgressDto::completedLessons).sum();
        return new UserEngagementDto(
                user.user().id(),
                user.user().displayName(),
                user.department().name(),
                courses.size(),
                completedLessons,
                courses.stream().mapToInt(UserCourseProgressDto::progressPercentage).average().orElse(0),
                !courses.isEmpty()
        );
    }

    private DashboardOverviewDto buildOverview(AnalyticsSnapshot snapshot) {
        long activeUsers = snapshot.userEngagement().stream().filter(UserEngagementDto::active).count();
        long completedCourses = snapshot.courseStats().stream().mapToLong(CourseStatsDto::completions).sum();
        double averageProgress = snapshot.userEngagement().stream()
                .mapToDouble(UserEngagementDto::averageProgress)
                .average()
                .orElse(0);
        double completionRate = snapshot.courseStats().stream()
                .mapToDouble(CourseStatsDto::completionRate)
                .average()
                .orElse(0);
        return new DashboardOverviewDto(
                snapshot.departments().size(),
                snapshot.users().size(),
                activeUsers,
                snapshot.courseStats().size(),
                completedCourses,
                round(averageProgress),
                round(completionRate),
                OffsetDateTime.now()
        );
    }

    private LearningReportDto toDto(LearningReport report) {
        try {
            Map<String, Object> data = objectMapper.readValue(report.getPayloadJson(), new TypeReference<>() {});
            return new LearningReportDto(
                    report.getId(),
                    report.getReportType(),
                    report.getGeneratedAt(),
                    report.getGeneratedBy(),
                    report.getFormat(),
                    data
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse analytics report", ex);
        }
    }

    private void requireAnalyticsRole(String rolesHeader) {
        Set<String> roles = parseRoles(rolesHeader);
        if (roles.stream().noneMatch(ANALYTICS_ROLES::contains)) {
            throw new AccessDeniedException("Analytics access requires ADMIN, HR, MANAGER or TECHNOLOG role");
        }
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Set.of();
        }
        Set<String> roles = new HashSet<>();
        for (String value : rolesHeader.split(",")) {
            String role = value.trim().toUpperCase(Locale.ROOT);
            if (!role.isBlank()) {
                roles.add(role);
            }
        }
        return roles;
    }

    private List<UserCourseProgressDto> safeCourses(ProgressSummaryDto summary) {
        return summary == null || summary.courses() == null ? List.of() : summary.courses();
    }

    private double averageProgress(List<ProgressSummaryDto> summaries) {
        return round(summaries.stream()
                .flatMap(summary -> safeCourses(summary).stream())
                .mapToInt(UserCourseProgressDto::progressPercentage)
                .average()
                .orElse(0));
    }

    private double completionRate(List<ProgressSummaryDto> summaries) {
        long total = summaries.stream().mapToLong(summary -> safeCourses(summary).size()).sum();
        long completed = summaries.stream()
                .flatMap(summary -> safeCourses(summary).stream())
                .filter(course -> course.progressPercentage() >= 100)
                .count();
        return percent(completed, total);
    }

    private double percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return round((part * 100.0) / total);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record UserWithDepartment(UserProfileDto user, DepartmentDto department) {
    }

    private record AnalyticsSnapshot(
            List<DepartmentDto> departments,
            List<UserWithDepartment> users,
            List<DepartmentProgressDto> departmentProgress,
            List<CourseStatsDto> courseStats,
            List<UserEngagementDto> userEngagement
    ) {
    }

    private static class CourseAccumulator {
        private final String courseId;
        private final String courseTitle;
        private long enrollments;
        private long completions;
        private long lessonsCompleted;
        private long progressSum;

        private CourseAccumulator(String courseId, String courseTitle) {
            this.courseId = courseId;
            this.courseTitle = courseTitle;
        }

        private void add(UserCourseProgressDto course) {
            enrollments++;
            lessonsCompleted += course.completedLessons();
            progressSum += course.progressPercentage();
            if (course.progressPercentage() >= 100) {
                completions++;
            }
        }

        private double averageProgress() {
            if (enrollments == 0) {
                return 0;
            }
            return Math.round((progressSum * 1.0 / enrollments) * 10.0) / 10.0;
        }
    }
}
