package org.example.courses.repository;

import org.example.courses.model.Enrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {

    @EntityGraph(attributePaths = {"course"})
    List<Enrollment> findByUserId(String userId);

    @EntityGraph(attributePaths = {"course"})
    List<Enrollment> findByUserIdIn(Collection<String> userIds);

    Optional<Enrollment> findFirstByUserIdAndCourse_IdOrderByEnrollmentDateDesc(String userId, String courseId);
}
