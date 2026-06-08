package org.example.courses.service;

import lombok.RequiredArgsConstructor;
import org.example.courses.dto.LearningHistoryDto;
import org.example.courses.dto.ProgressSummaryDto;
import org.example.courses.dto.UserCourseProgressDto;
import org.example.courses.kafka.CourseCompletedEvent;
import org.example.courses.kafka.EventPublisher;
import org.example.courses.kafka.LessonCompletedEvent;
import org.example.courses.model.*;
import org.example.courses.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final UserProgressRepository userProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final CertificateService certificateService;
    private final EventPublisher eventPublisher;

    public ProgressSummaryDto getMyProgress(String userId) {
        List<UserProgress> progresses = userProgressRepository.findByUserId(userId);
        Map<String, List<UserProgress>> byCourse = progresses.stream()
                .collect(Collectors.groupingBy(p -> p.getCourse().getId()));

        List<UserCourseProgressDto> courseDtos = new ArrayList<>();
        for (Map.Entry<String, List<UserProgress>> e : byCourse.entrySet()) {
            String courseId = e.getKey();
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) continue;

            int total = lessonRepository.findByModuleCourseId(courseId).size();
            int completed = (int) e.getValue().stream().filter(p -> p.getStatus() == ProgressStatus.COMPLETED).count();
            int lessonPercent = total == 0 ? 0 : (int) Math.round(100.0 * completed / total);
            int percent = Math.max(lessonPercent, dateBasedProgress(course));
            courseDtos.add(new UserCourseProgressDto(courseId, course.getTitle(), completed, total, percent));
        }

        // если ещё нет записей прогресса, всё равно покажем активные enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        Set<String> already = courseDtos.stream().map(UserCourseProgressDto::courseId).collect(Collectors.toSet());
        for (Enrollment en : enrollments) {
            String courseId = en.getCourse().getId();
            if (already.contains(courseId)) continue;
            int total = lessonRepository.findByModuleCourseId(courseId).size();
            int percent = switch (en.getStatus()) {
                case COMPLETED -> 100;
                case ACTIVE -> dateBasedProgress(en.getCourse());
                default -> 0;
            };
            courseDtos.add(new UserCourseProgressDto(courseId, en.getCourse().getTitle(), 0, total, percent));
            already.add(courseId);
        }

        return new ProgressSummaryDto(userId, courseDtos);
    }

    private int dateBasedProgress(Course course) {
        if (course == null || course.getStartDate() == null || course.getEndDate() == null) {
            return 0;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = course.getStartDate();
        LocalDate end = course.getEndDate();
        if (today.isBefore(start)) {
            return 0;
        }
        if (today.isAfter(end)) {
            return 100;
        }
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
        long elapsedDays = Math.max(1, ChronoUnit.DAYS.between(start, today) + 1);
        return (int) Math.min(100, Math.round(100.0 * elapsedDays / totalDays));
    }

    public ProgressSummaryDto getUserProgress(String userId) {
        return getMyProgress(userId);
    }

    public List<ProgressSummaryDto> getUsersProgress(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<String> distinctUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        if (distinctUserIds.isEmpty()) {
            return List.of();
        }

        Map<String, List<UserProgress>> progressByUser = userProgressRepository.findByUserIdIn(distinctUserIds).stream()
                .collect(Collectors.groupingBy(UserProgress::getUserId));
        Map<String, List<Enrollment>> enrollmentsByUser = enrollmentRepository.findByUserIdIn(distinctUserIds).stream()
                .collect(Collectors.groupingBy(Enrollment::getUserId));

        Set<String> courseIds = new HashSet<>();
        progressByUser.values().stream()
                .flatMap(Collection::stream)
                .map(progress -> progress.getCourse().getId())
                .forEach(courseIds::add);
        enrollmentsByUser.values().stream()
                .flatMap(Collection::stream)
                .map(enrollment -> enrollment.getCourse().getId())
                .forEach(courseIds::add);

        Map<String, Course> coursesById = courseIds.isEmpty()
                ? Map.of()
                : courseRepository.findAllById(courseIds).stream()
                        .collect(Collectors.toMap(Course::getId, course -> course));
        Map<String, Integer> lessonCountsByCourse = courseIds.isEmpty()
                ? Map.of()
                : lessonRepository.findByModuleCourseIdIn(courseIds).stream()
                        .collect(Collectors.groupingBy(
                                lesson -> lesson.getModule().getCourse().getId(),
                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                        ));

        return distinctUserIds.stream()
                .map(userId -> buildProgressSummary(
                        userId,
                        progressByUser.getOrDefault(userId, List.of()),
                        enrollmentsByUser.getOrDefault(userId, List.of()),
                        coursesById,
                        lessonCountsByCourse
                ))
                .toList();
    }

    private ProgressSummaryDto buildProgressSummary(
            String userId,
            List<UserProgress> progresses,
            List<Enrollment> enrollments,
            Map<String, Course> coursesById,
            Map<String, Integer> lessonCountsByCourse
    ) {
        Map<String, List<UserProgress>> byCourse = progresses.stream()
                .collect(Collectors.groupingBy(p -> p.getCourse().getId()));

        List<UserCourseProgressDto> courseDtos = new ArrayList<>();
        for (Map.Entry<String, List<UserProgress>> entry : byCourse.entrySet()) {
            String courseId = entry.getKey();
            Course course = coursesById.get(courseId);
            if (course == null) continue;

            int total = lessonCountsByCourse.getOrDefault(courseId, 0);
            int completed = (int) entry.getValue().stream()
                    .filter(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                    .count();
            int lessonPercent = total == 0 ? 0 : (int) Math.round(100.0 * completed / total);
            int percent = Math.max(lessonPercent, dateBasedProgress(course));
            courseDtos.add(new UserCourseProgressDto(courseId, course.getTitle(), completed, total, percent));
        }

        Set<String> already = courseDtos.stream().map(UserCourseProgressDto::courseId).collect(Collectors.toSet());
        for (Enrollment enrollment : enrollments) {
            String courseId = enrollment.getCourse().getId();
            if (already.contains(courseId)) continue;
            Course course = coursesById.getOrDefault(courseId, enrollment.getCourse());
            int total = lessonCountsByCourse.getOrDefault(courseId, 0);
            int percent = switch (enrollment.getStatus()) {
                case COMPLETED -> 100;
                case ACTIVE -> dateBasedProgress(course);
                default -> 0;
            };
            courseDtos.add(new UserCourseProgressDto(courseId, course.getTitle(), 0, total, percent));
            already.add(courseId);
        }

        return new ProgressSummaryDto(userId, courseDtos);
    }

    public List<LearningHistoryDto> getMyHistory(String userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return learningHistoryRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                .limit(safeLimit)
                .map(h -> new LearningHistoryDto(h.getId(), h.getAction(), h.getTimestamp(), h.getDetails()))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProgress completeLesson(String userId, String lessonId, Long timeSpentSecondsDelta) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));
        Course course = lesson.getModule().getCourse();

        UserProgress progress = userProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> UserProgress.builder()
                        .userId(userId)
                        .course(course)
                        .lesson(lesson)
                        .status(ProgressStatus.NOT_STARTED)
                        .progressPercentage(0)
                        .timeSpentSeconds(0L)
                        .build());

        long delta = timeSpentSecondsDelta == null ? 0L : Math.max(0L, timeSpentSecondsDelta);
        progress.setTimeSpentSeconds(progress.getTimeSpentSeconds() + delta);
        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setProgressPercentage(100);
        UserProgress saved = userProgressRepository.save(progress);

        learningHistoryRepository.save(LearningHistory.builder()
                .userId(userId)
                .action("LESSON_COMPLETED")
                .timestamp(OffsetDateTime.now())
                .details("lessonId=" + lessonId + ", courseId=" + course.getId())
                .build());

        eventPublisher.publishLessonCompleted(new LessonCompletedEvent(userId, course.getId(), lessonId, OffsetDateTime.now()));

        // реальный пересчёт прогресса по курсу
        recomputeCourseCompletion(userId, course.getId());
        return saved;
    }

    @Transactional
    protected void recomputeCourseCompletion(String userId, String courseId) {
        List<Lesson> lessons = lessonRepository.findByModuleCourseId(courseId);
        if (lessons.isEmpty()) return;

        Set<String> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        List<UserProgress> progresses = userProgressRepository.findByUserIdAndCourseId(userId, courseId);
        long completed = progresses.stream()
                .filter(p -> p.getStatus() == ProgressStatus.COMPLETED && lessonIds.contains(p.getLesson().getId()))
                .count();
        int percent = (int) Math.round(100.0 * completed / lessonIds.size());

        // обновим assignment/enrollment статусы
        if (percent >= 100) {
            // Enrollment -> COMPLETED
            enrollmentRepository.findByUserId(userId).stream()
                    .filter(e -> e.getCourse().getId().equals(courseId) && e.getStatus() != EnrollmentStatus.COMPLETED)
                    .forEach(e -> {
                        e.setStatus(EnrollmentStatus.COMPLETED);
                        e.setCompletionDate(OffsetDateTime.now());
                        enrollmentRepository.save(e);
                    });

            courseAssignmentRepository.findByUserId(userId).stream()
                    .filter(a -> a.getCourse().getId().equals(courseId) && a.getStatus() != AssignmentStatus.COMPLETED)
                    .forEach(a -> {
                        a.setStatus(AssignmentStatus.COMPLETED);
                        courseAssignmentRepository.save(a);
                    });

            learningHistoryRepository.save(LearningHistory.builder()
                    .userId(userId)
                    .action("COURSE_COMPLETED")
                    .timestamp(OffsetDateTime.now())
                    .details("courseId=" + courseId)
                    .build());

            eventPublisher.publishCourseCompleted(new CourseCompletedEvent(userId, courseId, OffsetDateTime.now()));
            certificateService.issueIfNeeded(userId, courseId);
        } else {
            courseAssignmentRepository.findByUserId(userId).stream()
                    .filter(a -> a.getCourse().getId().equals(courseId) && a.getStatus() == AssignmentStatus.ASSIGNED)
                    .forEach(a -> {
                        a.setStatus(AssignmentStatus.IN_PROGRESS);
                        courseAssignmentRepository.save(a);
                    });
        }
    }
}
