package org.example.courses.repository;

import org.example.courses.model.AssignmentRequestStatus;
import org.example.courses.model.CourseAssignmentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface CourseAssignmentRequestRepository extends JpaRepository<CourseAssignmentRequestEntity, String> {
    List<CourseAssignmentRequestEntity> findByRequestedByOrderByCreatedAtDesc(String requestedBy);

    List<CourseAssignmentRequestEntity> findByStatusOrderByCreatedAtDesc(AssignmentRequestStatus status);

    long countByUserIdAndStatus(String userId, AssignmentRequestStatus status);

    long countByUserIdAndStatusAndCreatedAtBetween(
            String userId,
            AssignmentRequestStatus status,
            OffsetDateTime from,
            OffsetDateTime to
    );

    boolean existsByUserIdAndCourse_IdAndStatus(String userId, String courseId, AssignmentRequestStatus status);
}
