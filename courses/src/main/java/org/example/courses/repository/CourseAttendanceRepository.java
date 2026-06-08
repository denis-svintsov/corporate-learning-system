package org.example.courses.repository;

import org.example.courses.model.CourseAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CourseAttendanceRepository extends JpaRepository<CourseAttendance, String> {
    List<CourseAttendance> findByCourseIdAndAttendanceDate(String courseId, LocalDate attendanceDate);

    Optional<CourseAttendance> findByCourseIdAndUserIdAndAttendanceDate(
            String courseId,
            String userId,
            LocalDate attendanceDate
    );
}
