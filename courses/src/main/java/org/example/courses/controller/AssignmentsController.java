package org.example.courses.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.courses.dto.AssignmentRequestDecisionRequest;
import org.example.courses.dto.AssignmentRequestDto;
import org.example.courses.dto.AssignmentPolicyDto;
import org.example.courses.dto.ConfirmCourseCompletionRequest;
import org.example.courses.dto.CourseAssignmentDto;
import org.example.courses.dto.CourseAssignmentRequest;
import org.example.courses.dto.CourseDto;
import org.example.courses.dto.SubmitAssignmentRequest;
import org.example.courses.dto.UpdateAssignmentPolicyRequest;
import org.example.courses.model.AssignmentRequestStatus;
import org.example.courses.model.CourseAssignment;
import org.example.courses.service.AssignmentService;
import org.example.courses.service.CourseService;
import org.example.courses.util.CsvUtil;
import org.example.courses.util.SecurityHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AssignmentsController {

    private final AssignmentService assignmentService;
    private final CourseService courseService;

    /**
     * POST /courses/assign (по ТЗ)
     */
    @PostMapping("/courses/assign")
    public CourseAssignment assign(
            @Valid @RequestBody CourseAssignmentRequest req,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        requireAdminOrHr(parseRoles(rolesHeader));
        try {
            return assignmentService.assign(req);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    @PostMapping("/courses/assignment-requests")
    public AssignmentRequestDto submitRequest(
            @Valid @RequestBody SubmitAssignmentRequest req,
            @RequestHeader(name = "X-User-Id", required = false) String requesterId
    ) {
        if (requesterId == null || requesterId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        if (!requesterId.equals(req.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can submit requests only for yourself");
        }
        try {
            return assignmentService.submitRequest(new SubmitAssignmentRequest(
                    req.userId(),
                    req.courseId(),
                    requesterId,
                    req.comment(),
                    req.dueDate()
            ));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    @GetMapping("/courses/assignment-requests/my")
    public List<AssignmentRequestDto> myRequests(
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        return assignmentService.getMyRequests(userId);
    }

    @GetMapping("/courses/assignment-requests")
    public List<AssignmentRequestDto> requests(
            @RequestParam(required = false) AssignmentRequestStatus status,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        requireAdminOrHr(parseRoles(rolesHeader));
        return assignmentService.getRequests(status);
    }

    @GetMapping("/courses/assignment-policy")
    public AssignmentPolicyDto getAssignmentPolicy() {
        return assignmentService.getAssignmentPolicy();
    }

    @GetMapping("/internal/courses/{courseId}/assignments/{userId}/exists")
    public boolean hasAssignment(
            @PathVariable String courseId,
            @PathVariable String userId
    ) {
        return assignmentService.hasAssignment(userId, courseId);
    }

    @PutMapping("/courses/assignment-policy")
    public AssignmentPolicyDto updateAssignmentPolicy(
            @Valid @RequestBody UpdateAssignmentPolicyRequest req,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        requireAdminOrHr(parseRoles(rolesHeader));
        return assignmentService.updateAssignmentPolicy(req.maxCoursesPerQuarter());
    }

    @PostMapping("/courses/assignment-requests/{requestId}/approve")
    public AssignmentRequestDto approveRequest(
            @PathVariable String requestId,
            @RequestHeader(name = "X-User-Id", required = false) String reviewerId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @RequestBody(required = false) AssignmentRequestDecisionRequest decision
    ) {
        if (reviewerId == null || reviewerId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        requireAdminOrHr(parseRoles(rolesHeader));
        try {
            return assignmentService.approveRequest(requestId, reviewerId, decision);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    @PostMapping("/courses/assignment-requests/{requestId}/reject")
    public AssignmentRequestDto rejectRequest(
            @PathVariable String requestId,
            @RequestHeader(name = "X-User-Id", required = false) String reviewerId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @RequestBody(required = false) AssignmentRequestDecisionRequest decision
    ) {
        if (reviewerId == null || reviewerId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        requireAdminOrHr(parseRoles(rolesHeader));
        try {
            return assignmentService.rejectRequest(requestId, reviewerId, decision);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    @PostMapping("/courses/{courseId}/complete")
    public CourseAssignmentDto confirmCompletion(
            @PathVariable String courseId,
            @Valid @RequestBody ConfirmCourseCompletionRequest request,
            @RequestHeader(name = "X-User-Id", required = false) String reviewerId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        if (reviewerId == null || reviewerId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        Set<String> roles = SecurityHeaders.parseRoles(rolesHeader);
        requireCompletionAccess(courseId, reviewerId, roles);
        CourseAssignment assignment = assignmentService.confirmCompletion(
                courseId,
                request.userId(),
                reviewerId,
                request.passed() == null || request.passed()
        );
        return toDto(assignment);
    }

    /**
     * GET /users/{userId}/assigned-courses (по ТЗ)
     */
    @GetMapping("/users/{userId}/assigned-courses")
    public List<CourseAssignmentDto> assigned(@PathVariable String userId) {
        return toDtoList(userId);
    }

    /**
     * GET /courses/assigned-courses/my (через gateway, userId из заголовка)
     */
    @GetMapping("/courses/assigned-courses/my")
    public List<CourseAssignmentDto> myAssigned(
            @RequestHeader(name = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        return toDtoList(userId);
    }

    private List<CourseAssignmentDto> toDtoList(String userId) {
        return assignmentService.getAssignedCourses(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private CourseAssignmentDto toDto(CourseAssignment a) {
        return new CourseAssignmentDto(
                a.getId(),
                a.getUserId(),
                a.getCourse() != null ? a.getCourse().getId() : null,
                a.getCourse() != null ? a.getCourse().getTitle() : null,
                a.getCourse() != null ? a.getCourse().getDescription() : null,
                a.getCourse() != null && a.getCourse().getCategory() != null ? a.getCourse().getCategory().getId() : null,
                a.getCourse() != null ? a.getCourse().getDifficulty() : null,
                a.getCourse() != null ? a.getCourse().getDurationMinutes() : null,
                a.getCourse() != null ? a.getCourse().getStatus() : null,
                a.getCourse() != null ? a.getCourse().getCoverUrl() : null,
                a.getCourse() != null ? a.getCourse().getAggregatorUrl() : null,
                a.getCourse() != null ? a.getCourse().getInstructions() : null,
                a.getCourse() != null ? CsvUtil.splitToSet(a.getCourse().getSpecializationsCsv()) : java.util.Set.of(),
                a.getCourse() != null ? a.getCourse().getCompanyCost() : null,
                a.getCourse() != null ? a.getCourse().getStartDate() : null,
                a.getCourse() != null ? a.getCourse().getEndDate() : null,
                a.getAssignedBy(),
                a.getDueDate(),
                a.getStatus(),
                a.getCreatedAt()
        );
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private void requireAdminOrHr(Set<String> roles) {
        if (!roles.contains("ADMIN") && !roles.contains("HR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied (HR/Admin only)");
        }
    }

    private void requireCompletionAccess(String courseId, String userId, Set<String> roles) {
        if (SecurityHeaders.hasAnyRole(roles, "ADMIN", "HR", "TECHNOLOG")) {
            return;
        }
        if (!roles.contains("EXPERT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        CourseDto course = courseService.getDtoById(courseId);
        if (!course.expertIds().contains(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Expert is not assigned to this course");
        }
    }
}
