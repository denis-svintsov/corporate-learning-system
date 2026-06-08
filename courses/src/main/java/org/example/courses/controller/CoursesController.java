package org.example.courses.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.courses.dto.CreateCourseRequest;
import org.example.courses.dto.AttendanceDto;
import org.example.courses.dto.CourseCoverUploadDto;
import org.example.courses.dto.CourseDto;
import org.example.courses.dto.CourseParticipantDto;
import org.example.courses.dto.MarkAttendanceRequest;
import org.example.courses.dto.UpdateCourseRequest;
import org.example.courses.model.CourseStatus;
import org.example.courses.model.DifficultyLevel;
import org.example.courses.service.AccessControlService;
import org.example.courses.service.AssignmentService;
import org.example.courses.service.CourseAttendanceService;
import org.example.courses.service.CourseCoverStorageService;
import org.example.courses.service.CourseService;
import org.example.courses.users.UsersServiceClient;
import org.example.courses.util.SecurityHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CoursesController {

    private final CourseService courseService;
    private final AccessControlService accessControlService;
    private final UsersServiceClient usersServiceClient;
    private final AssignmentService assignmentService;
    private final CourseAttendanceService courseAttendanceService;
    private final CourseCoverStorageService courseCoverStorageService;

    /**
     * Каталог курсов с поиском и фильтрацией.
     */
    @GetMapping
    public Page<CourseDto> catalog(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String categoryId,
                                   @RequestParam(required = false) DifficultyLevel difficulty,
                                   @RequestParam(required = false) CourseStatus status,
                                   @RequestParam(required = false) String tagId,
                                   @RequestParam(required = false) String positionId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestHeader(name = "X-User-Id", required = false) String userId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        var pageResult = courseService.catalogDto(q, categoryId, difficulty, status, tagId, positionId, pageable);

        if (userId == null || userId.isBlank()) {
            return pageResult;
        }

        // Упрощённая фильтрация "после выборки" — корректнее делать это через join/spec,
        // но для MVP так быстрее.
        List<CourseDto> filtered = pageResult.getContent().stream()
                .filter(c -> accessControlService.canAccessCourse(userId, c.allowedRoles(), c.allowedDepartmentIds()))
                .collect(Collectors.toList());

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    /**
     * Рекомендованные курсы по должности сотрудника.
     */
    @GetMapping("/recommended")
    public Page<CourseDto> recommended(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestHeader(name = "X-User-Id", required = false) String userId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        if (userId == null || userId.isBlank()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        var user = usersServiceClient.getUserContext(userId);
        String positionId = user != null ? user.positionId() : null;
        if (positionId == null || positionId.isBlank()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        var pageResult = courseService.catalogDto(null, null, null, CourseStatus.ACTIVE, null, positionId, pageable);
        var assignedCourseIds = assignmentService.getAssignedCourses(userId).stream()
                .map(a -> a.getCourse() != null ? a.getCourse().getId() : null)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        List<CourseDto> filtered = pageResult.getContent().stream()
                .filter(c -> !assignedCourseIds.contains(c.id()))
                .filter(c -> accessControlService.canAccessCourse(userId, c.allowedRoles(), c.allowedDepartmentIds()))
                .collect(Collectors.toList());
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    @GetMapping("/manage")
    public Page<CourseDto> manageCatalog(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) DifficultyLevel difficulty,
                                         @RequestParam(required = false) CourseStatus status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "100") int size,
                                         @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader) {
        Set<String> roles = SecurityHeaders.parseRoles(rolesHeader);
        requireAnyRole(roles, "ADMIN", "HR", "TECHNOLOG", "EXPERT");
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        return courseService.catalogDto(q, null, difficulty, status, null, null, pageable);
    }

    @GetMapping("/{id}")
    public CourseDto get(@PathVariable String id,
                         @RequestHeader(name = "X-User-Id", required = false) String userId) {
        var course = courseService.getDtoById(id);
        if (userId != null && !accessControlService.canAccessCourse(userId, course.allowedRoles(), course.allowedDepartmentIds())) {
            throw new IllegalArgumentException("Access denied to course: " + id);
        }
        return course;
    }

    /**
     * Создать курс (HR/Admin).
     */
    @PostMapping
    public CourseDto create(@Valid @RequestBody CreateCourseRequest req,
                            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader) {
        requireCourseEditor(SecurityHeaders.parseRoles(rolesHeader));
        return courseService.getDtoById(courseService.create(req).getId());
    }

    @PutMapping("/{id}")
    public CourseDto update(@PathVariable String id,
                            @Valid @RequestBody UpdateCourseRequest req,
                            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader) {
        requireCourseEditor(SecurityHeaders.parseRoles(rolesHeader));
        return courseService.getDtoById(courseService.update(id, req).getId());
    }

    @PostMapping(value = "/covers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CourseCoverUploadDto uploadCover(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        requireCourseEditor(SecurityHeaders.parseRoles(rolesHeader));
        String objectKey = courseCoverStorageService.uploadCover(file);
        return new CourseCoverUploadDto(objectKey, "/courses/covers/" + objectKey);
    }

    @GetMapping("/covers/{objectKey}")
    public ResponseEntity<byte[]> downloadCover(@PathVariable String objectKey) {
        var stored = courseCoverStorageService.downloadCover(objectKey);
        try (var inputStream = stored.inputStream()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(stored.contentType()))
                    .body(inputStream.readAllBytes());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read course cover", ex);
        }
    }

    @GetMapping("/{id}/participants")
    public List<CourseParticipantDto> participants(
            @PathVariable String id,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        requireAnyRole(SecurityHeaders.parseRoles(rolesHeader), "ADMIN", "HR", "TECHNOLOG", "EXPERT");
        return courseAttendanceService.participants(id);
    }

    @GetMapping("/{id}/attendance")
    public List<AttendanceDto> attendance(
            @PathVariable String id,
            @RequestParam(required = false) LocalDate date,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        requireAnyRole(SecurityHeaders.parseRoles(rolesHeader), "ADMIN", "HR", "TECHNOLOG", "EXPERT");
        return courseAttendanceService.attendance(id, date);
    }

    @PostMapping("/{id}/attendance")
    public AttendanceDto markAttendance(
            @PathVariable String id,
            @Valid @RequestBody MarkAttendanceRequest request,
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        requireAnyRole(SecurityHeaders.parseRoles(rolesHeader), "ADMIN", "HR", "TECHNOLOG", "EXPERT");
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        return courseAttendanceService.mark(id, userId, request);
    }

    private void requireCourseEditor(Set<String> roles) {
        requireAnyRole(roles, "ADMIN", "TECHNOLOG");
    }

    private void requireAnyRole(Set<String> roles, String... allowedRoles) {
        if (!SecurityHeaders.hasAnyRole(roles, allowedRoles)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

}
