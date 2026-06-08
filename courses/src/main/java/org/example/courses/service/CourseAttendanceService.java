package org.example.courses.service;

import lombok.RequiredArgsConstructor;
import org.example.courses.dto.AttendanceDto;
import org.example.courses.dto.CourseParticipantDto;
import org.example.courses.dto.MarkAttendanceRequest;
import org.example.courses.model.AttendanceStatus;
import org.example.courses.model.Course;
import org.example.courses.model.CourseAttendance;
import org.example.courses.repository.CourseAssignmentRepository;
import org.example.courses.repository.CourseAttendanceRepository;
import org.example.courses.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseAttendanceService {

    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final CourseAttendanceRepository attendanceRepository;

    public List<CourseParticipantDto> participants(String courseId) {
        Course course = getCourse(courseId);
        long totalCourseDays = courseDurationDays(course);
        Map<String, Set<LocalDate>> presentDatesByUser = attendanceRepository
                .findByCourseIdAndStatus(courseId, AttendanceStatus.PRESENT)
                .stream()
                .collect(Collectors.groupingBy(
                        CourseAttendance::getUserId,
                        Collectors.mapping(CourseAttendance::getAttendanceDate, Collectors.toSet())
                ));
        return assignmentRepository.findByCourse_Id(courseId).stream()
                .map(assignment -> {
                    Set<LocalDate> presentDates = presentDatesByUser.getOrDefault(assignment.getUserId(), Set.of());
                    long presentDays = presentDates.stream().filter(Objects::nonNull).distinct().count();
                    int attendancePercentage = attendancePercentage(presentDays, totalCourseDays);
                    return new CourseParticipantDto(
                            assignment.getUserId(),
                            assignment.getId(),
                            assignment.getStatus(),
                            assignment.getDueDate(),
                            assignment.getCreatedAt(),
                            presentDays,
                            totalCourseDays,
                            attendancePercentage
                    );
                })
                .toList();
    }

    public List<AttendanceDto> attendance(String courseId, LocalDate date) {
        ensureCourseExists(courseId);
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return attendanceRepository.findByCourseIdAndAttendanceDate(courseId, targetDate).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AttendanceDto mark(String courseId, String markedBy, MarkAttendanceRequest request) {
        ensureCourseExists(courseId);
        CourseAttendance attendance = attendanceRepository
                .findByCourseIdAndUserIdAndAttendanceDate(courseId, request.userId(), request.attendanceDate())
                .orElseGet(CourseAttendance::new);
        attendance.setCourseId(courseId);
        attendance.setUserId(request.userId());
        attendance.setAttendanceDate(request.attendanceDate());
        attendance.setStatus(request.status());
        attendance.setComment(request.comment());
        attendance.setMarkedBy(markedBy);
        return toDto(attendanceRepository.save(attendance));
    }

    private void ensureCourseExists(String courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found: " + courseId);
        }
    }

    private Course getCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    private long courseDurationDays(Course course) {
        if (course.getStartDate() == null || course.getEndDate() == null || course.getEndDate().isBefore(course.getStartDate())) {
            return 0;
        }
        return Math.max(1, ChronoUnit.DAYS.between(course.getStartDate(), course.getEndDate()) + 1);
    }

    private int attendancePercentage(long presentDays, long totalCourseDays) {
        if (presentDays <= 0 || totalCourseDays <= 0) {
            return 0;
        }
        long countedDays = Math.min(presentDays, totalCourseDays);
        return (int) Math.min(100, Math.round(100.0 * countedDays / totalCourseDays));
    }

    private AttendanceDto toDto(CourseAttendance attendance) {
        return new AttendanceDto(
                attendance.getId(),
                attendance.getCourseId(),
                attendance.getUserId(),
                attendance.getAttendanceDate(),
                attendance.getStatus(),
                attendance.getComment(),
                attendance.getMarkedBy(),
                attendance.getUpdatedAt()
        );
    }
}
