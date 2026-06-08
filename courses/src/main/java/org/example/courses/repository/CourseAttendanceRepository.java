package org.example.courses.repository;

import org.example.courses.model.CourseAttendance;
import org.example.courses.model.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseAttendanceRepository extends JpaRepository<CourseAttendance, String> {
    List<CourseAttendance> findByCourseIdAndAttendanceDate(String courseId, LocalDate attendanceDate);

    Optional<CourseAttendance> findByCourseIdAndUserIdAndAttendanceDate(
            String courseId,
            String userId,
            LocalDate attendanceDate
    );

    List<CourseAttendance> findByUserIdAndStatus(String userId, AttendanceStatus status);

    List<CourseAttendance> findByUserIdInAndStatus(Collection<String> userIds, AttendanceStatus status);

    List<CourseAttendance> findByCourseIdAndStatus(String courseId, AttendanceStatus status);
}
