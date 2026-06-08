package org.example.courses.service;

import lombok.RequiredArgsConstructor;
import org.example.courses.dto.AssignmentRequestDecisionRequest;
import org.example.courses.dto.AssignmentRequestDto;
import org.example.courses.dto.AssignmentPolicyDto;
import org.example.courses.dto.CourseAssignmentRequest;
import org.example.courses.dto.SubmitAssignmentRequest;
import org.example.courses.model.AssignmentPolicy;
import org.example.courses.kafka.AssignmentRequestedEvent;
import org.example.courses.kafka.CourseAssignedEvent;
import org.example.courses.kafka.EventPublisher;
import org.example.courses.model.AssignmentRequestStatus;
import org.example.courses.model.AssignmentStatus;
import org.example.courses.model.Course;
import org.example.courses.model.CourseAssignment;
import org.example.courses.model.CourseAssignmentRequestEntity;
import org.example.courses.model.Enrollment;
import org.example.courses.model.EnrollmentStatus;
import org.example.courses.model.LearningHistory;
import org.example.courses.kafka.CourseCompletedEvent;
import org.example.courses.repository.AssignmentPolicyRepository;
import org.example.courses.repository.CourseAssignmentRequestRepository;
import org.example.courses.repository.CourseAssignmentRepository;
import org.example.courses.repository.EnrollmentRepository;
import org.example.courses.repository.CourseRepository;
import org.example.courses.repository.LearningHistoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {
    private static final List<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES = Arrays.asList(
            AssignmentStatus.ASSIGNED,
            AssignmentStatus.IN_PROGRESS
    );

    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final CourseAssignmentRequestRepository courseAssignmentRequestRepository;
    private final AssignmentPolicyRepository assignmentPolicyRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final CertificateService certificateService;
    private final EventPublisher eventPublisher;

    @Transactional
    public CourseAssignment assign(CourseAssignmentRequest req) {
        enforceQuarterLimit(req.userId());
        return assignInternal(req.userId(), req.courseId(), req.assignedBy(), req.dueDate());
    }

    @Transactional
    public AssignmentRequestDto submitRequest(SubmitAssignmentRequest req) {
        enforceQuarterLimit(req.userId());
        if (courseAssignmentRepository.existsByUserIdAndCourse_Id(req.userId(), req.courseId())) {
            throw new IllegalStateException("Course already assigned to this user.");
        }
        if (courseAssignmentRequestRepository.existsByUserIdAndCourse_IdAndStatus(req.userId(), req.courseId(), AssignmentRequestStatus.PENDING)) {
            throw new IllegalStateException("Assignment request for this course is already pending.");
        }
        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + req.courseId()));

        CourseAssignmentRequestEntity request = CourseAssignmentRequestEntity.builder()
                .userId(req.userId())
                .course(course)
                .requestedBy(req.requestedBy())
                .comment(req.comment())
                .dueDate(req.dueDate())
                .status(AssignmentRequestStatus.PENDING)
                .build();
        CourseAssignmentRequestEntity saved = courseAssignmentRequestRepository.save(request);
        eventPublisher.publishAssignmentRequested(new AssignmentRequestedEvent(
                saved.getId(),
                saved.getUserId(),
                course.getId(),
                course.getTitle(),
                saved.getCreatedAt()
        ));
        return toRequestDto(saved);
    }

    @Transactional
    public AssignmentRequestDto approveRequest(String requestId, String reviewerId, AssignmentRequestDecisionRequest decision) {
        CourseAssignmentRequestEntity request = courseAssignmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
        if (request.getStatus() != AssignmentRequestStatus.PENDING) {
            throw new IllegalStateException("Request already reviewed.");
        }

        assignInternal(
                request.getUserId(),
                request.getCourse().getId(),
                reviewerId,
                decision != null && decision.dueDate() != null ? decision.dueDate() : request.getDueDate()
        );

        request.setStatus(AssignmentRequestStatus.APPROVED);
        request.setReviewedBy(reviewerId);
        request.setReviewerComment(decision != null ? decision.reviewerComment() : null);
        request.setReviewedAt(OffsetDateTime.now());
        return toRequestDto(courseAssignmentRequestRepository.save(request));
    }

    @Transactional
    public AssignmentRequestDto rejectRequest(String requestId, String reviewerId, AssignmentRequestDecisionRequest decision) {
        CourseAssignmentRequestEntity request = courseAssignmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
        if (request.getStatus() != AssignmentRequestStatus.PENDING) {
            throw new IllegalStateException("Request already reviewed.");
        }
        request.setStatus(AssignmentRequestStatus.REJECTED);
        request.setReviewedBy(reviewerId);
        request.setReviewerComment(decision != null ? decision.reviewerComment() : null);
        request.setReviewedAt(OffsetDateTime.now());
        return toRequestDto(courseAssignmentRequestRepository.save(request));
    }

    public List<AssignmentRequestDto> getMyRequests(String requestedBy) {
        return courseAssignmentRequestRepository.findByRequestedByOrderByCreatedAtDesc(requestedBy).stream()
                .map(this::toRequestDto)
                .toList();
    }

    public List<AssignmentRequestDto> getRequests(AssignmentRequestStatus status) {
        List<CourseAssignmentRequestEntity> requests = status == null
                ? courseAssignmentRequestRepository.findAll()
                : courseAssignmentRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        return requests.stream()
                .map(this::toRequestDto)
                .toList();
    }

    @Transactional
    public AssignmentPolicyDto updateAssignmentPolicy(int maxCoursesPerQuarter) {
        AssignmentPolicy policy = getOrCreatePolicy();
        policy.setMaxCoursesPerQuarter(maxCoursesPerQuarter);
        AssignmentPolicy saved = assignmentPolicyRepository.save(policy);
        return toPolicyDto(saved);
    }

    public AssignmentPolicyDto getAssignmentPolicy() {
        return toPolicyDto(getOrCreatePolicy());
    }

    public boolean hasAssignment(String userId, String courseId) {
        if (userId == null || userId.isBlank() || courseId == null || courseId.isBlank()) {
            return false;
        }
        return courseAssignmentRepository.existsByUserIdAndCourse_Id(userId, courseId);
    }

    @Transactional
    public CourseAssignment confirmCompletion(String courseId, String userId, String confirmedBy, boolean passed) {
        CourseAssignment assignment = courseAssignmentRepository.findByUserIdAndCourse_Id(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found for user and course."));
        Course course = assignment.getCourse();
        if (course == null) {
            course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        }
        final Course completionCourse = course;

        assignment.setStatus(passed ? AssignmentStatus.COMPLETED : AssignmentStatus.FAILED);
        CourseAssignment saved = courseAssignmentRepository.save(assignment);

        Enrollment enrollment = enrollmentRepository
                .findFirstByUserIdAndCourse_IdOrderByEnrollmentDateDesc(userId, courseId)
                .orElseGet(() -> Enrollment.builder()
                        .userId(userId)
                        .course(completionCourse)
                        .status(EnrollmentStatus.ACTIVE)
                        .build());
        enrollment.setStatus(passed ? EnrollmentStatus.COMPLETED : EnrollmentStatus.CANCELLED);
        enrollment.setCompletionDate(OffsetDateTime.now());
        enrollmentRepository.save(enrollment);

        if (passed) {
            certificateService.issueIfNeeded(userId, courseId);
        }

        learningHistoryRepository.save(LearningHistory.builder()
                .userId(userId)
                .action(passed ? "COURSE_COMPLETED_CONFIRMED" : "COURSE_FAILED_CONFIRMED")
                .details((passed ? "Курс пройден: \"" : "Курс не пройден: \"") + completionCourse.getTitle() + "\". Решение подтвердил пользователь " + confirmedBy)
                .build());

        if (passed) {
            eventPublisher.publishCourseCompleted(new CourseCompletedEvent(userId, courseId, OffsetDateTime.now()));
        }
        return saved;
    }

    private CourseAssignment assignInternal(String userId, String courseId, String assignedBy, java.time.LocalDate dueDate) {
        if (courseAssignmentRepository.existsByUserIdAndCourse_Id(userId, courseId)) {
            throw new IllegalStateException("Course already assigned to this user.");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        CourseAssignment assignment = CourseAssignment.builder()
                .userId(userId)
                .course(course)
                .assignedBy(assignedBy)
                .dueDate(dueDate)
                .status(AssignmentStatus.ASSIGNED)
                .build();
        CourseAssignment saved = courseAssignmentRepository.save(assignment);

        // На назначение создаём enrollment, если его ещё нет (упрощённо: допускаем дубликаты только в assignment,
        // но enrollment используем как "активную запись").
        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        enrollmentRepository.save(enrollment);

        eventPublisher.publishCourseAssigned(new CourseAssignedEvent(userId, courseId, assignedBy, OffsetDateTime.now()));
        return saved;
    }

    @Transactional
    public List<CourseAssignment> getAssignedCourses(String userId) {
        courseAssignmentRepository.markOverdueByDateForUser(userId, LocalDate.now(ZoneOffset.UTC));
        return courseAssignmentRepository.findByUserId(userId);
    }

    public long countCurrentQuarterAssigned(String userId) {
        QuarterWindow w = currentQuarterWindow();
        long activeInQuarter = courseAssignmentRepository.countByUserIdAndCreatedAtBetweenAndStatusIn(
                userId,
                w.start(),
                w.end(),
                ACTIVE_ASSIGNMENT_STATUSES
        );
        long undatedActiveFromPreviousQuarters = courseAssignmentRepository.countUndatedActiveBeforeQuarter(
                userId,
                w.start(),
                ACTIVE_ASSIGNMENT_STATUSES
        );
        return activeInQuarter + undatedActiveFromPreviousQuarters;
    }

    private void enforceQuarterLimit(String userId) {
        long assignedCount = countCurrentQuarterAssigned(userId);
        QuarterWindow w = currentQuarterWindow();
        long pendingCount = courseAssignmentRequestRepository.countByUserIdAndStatusAndCreatedAtBetween(
                userId,
                AssignmentRequestStatus.PENDING,
                w.start(),
                w.end()
        );
        int limit = getQuarterLimit();
        long count = assignedCount + pendingCount;
        if (count >= limit) {
            throw new IllegalStateException("Quarter limit reached. You can select up to " + limit + " courses per quarter.");
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    public void markOverdueAssignments() {
        courseAssignmentRepository.markOverdueByDate(LocalDate.now(ZoneOffset.UTC));
    }

    private QuarterWindow currentQuarterWindow() {
        OffsetDateTime now = OffsetDateTime.now();
        int month = now.getMonthValue();
        int startMonth = ((month - 1) / 3) * 3 + 1;
        OffsetDateTime start = OffsetDateTime.of(
                now.getYear(),
                startMonth,
                1,
                0, 0, 0, 0,
                now.getOffset()
        );
        return new QuarterWindow(start, start.plusMonths(3));
    }

    private record QuarterWindow(OffsetDateTime start, OffsetDateTime end) {}

    private int getQuarterLimit() {
        return getOrCreatePolicy().getMaxCoursesPerQuarter();
    }

    private AssignmentPolicy getOrCreatePolicy() {
        return assignmentPolicyRepository.findById(1)
                .orElseGet(() -> assignmentPolicyRepository.save(
                        AssignmentPolicy.builder()
                                .id(1)
                                .maxCoursesPerQuarter(3)
                                .build()
                ));
    }

    private AssignmentPolicyDto toPolicyDto(AssignmentPolicy policy) {
        return new AssignmentPolicyDto(policy.getMaxCoursesPerQuarter(), policy.getUpdatedAt());
    }

    private AssignmentRequestDto toRequestDto(CourseAssignmentRequestEntity req) {
        Course course = req.getCourse();
        return new AssignmentRequestDto(
                req.getId(),
                req.getUserId(),
                req.getRequestedBy(),
                course != null ? course.getId() : null,
                course != null ? course.getTitle() : null,
                course != null ? course.getDifficulty() : null,
                course != null ? course.getDurationMinutes() : null,
                course != null ? course.getCompanyCost() : null,
                req.getComment(),
                req.getDueDate(),
                req.getStatus(),
                req.getReviewedBy(),
                req.getReviewerComment(),
                req.getCreatedAt(),
                req.getReviewedAt()
        );
    }
}
