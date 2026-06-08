package org.example.courses.repository;

import org.example.courses.model.CourseAssignment;
import org.example.courses.model.AssignmentStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, String> {

    List<CourseAssignment> findByUserId(String userId);

    List<CourseAssignment> findByCourse_Id(String courseId);

    Optional<CourseAssignment> findByUserIdAndCourse_Id(String userId, String courseId);

    List<CourseAssignment> findByUserIdAndStatusIn(String userId, List<AssignmentStatus> statuses);

    List<CourseAssignment> findByUserIdAndCreatedAtBetween(String userId, OffsetDateTime from, OffsetDateTime to);

    long countByUserIdAndCreatedAtBetweenAndStatusIn(
            String userId,
            OffsetDateTime from,
            OffsetDateTime to,
            List<AssignmentStatus> statuses
    );

    @Query("""
            select count(a) from CourseAssignment a
            where a.userId = :userId
              and a.status in :statuses
              and a.createdAt < :quarterStart
              and a.dueDate is null
              and a.course.endDate is null
            """)
    long countUndatedActiveBeforeQuarter(
            @Param("userId") String userId,
            @Param("quarterStart") OffsetDateTime quarterStart,
            @Param("statuses") List<AssignmentStatus> statuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    update courses.course_assignment ca
                       set status = 'OVERDUE'
                      from courses.course c
                     where ca.course_id = c.id
                       and ca.status in ('ASSIGNED', 'IN_PROGRESS')
                       and (
                            (ca.due_date is not null and ca.due_date < :today)
                            or
                            (ca.due_date is null and c.end_date is not null and c.end_date < :today)
                       )
                    """,
            nativeQuery = true
    )
    int markOverdueByDate(
            @Param("today") LocalDate today
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    update courses.course_assignment ca
                       set status = 'OVERDUE'
                      from courses.course c
                     where ca.course_id = c.id
                       and ca.user_id = :userId
                       and ca.status in ('ASSIGNED', 'IN_PROGRESS')
                       and (
                            (ca.due_date is not null and ca.due_date < :today)
                            or
                            (ca.due_date is null and c.end_date is not null and c.end_date < :today)
                       )
                    """,
            nativeQuery = true
    )
    int markOverdueByDateForUser(
            @Param("userId") String userId,
            @Param("today") LocalDate today
    );

    boolean existsByUserIdAndCourse_Id(String userId, String courseId);
}
