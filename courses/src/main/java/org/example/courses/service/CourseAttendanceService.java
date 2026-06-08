package org.example.courses.service;

import lombok.RequiredArgsConstructor;
import org.example.courses.dto.AttendanceDto;
import org.example.courses.dto.CourseParticipantDto;
import org.example.courses.dto.MarkAttendanceRequest;
import org.example.courses.model.CourseAttendance;
import org.example.courses.repository.CourseAssignmentRepository;
import org.example.courses.repository.CourseAttendanceRepository;
import org.example.courses.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseAttendanceService {

    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final CourseAttendanceRepository attendanceRepository;

    public List<CourseParticipantDto> participants(String courseId) {
        ensureCourseExists(courseId);
        return assignmentRepository.findByCourse_Id(courseId).stream()
                .map(assignment -> new CourseParticipantDto(
                        assignment.getUserId(),
                        assignment.getId(),
                        assignment.getStatus(),
                        assignment.getDueDate(),
                        assignment.getCreatedAt()
                ))
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
