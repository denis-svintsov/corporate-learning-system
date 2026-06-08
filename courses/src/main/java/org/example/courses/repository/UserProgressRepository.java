package org.example.courses.repository;

import org.example.courses.model.UserProgress;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface UserProgressRepository extends JpaRepository<UserProgress, String> {

    @EntityGraph(attributePaths = {"course", "lesson", "lesson.module", "lesson.module.course"})
    List<UserProgress> findByUserId(String userId);

    @EntityGraph(attributePaths = {"course", "lesson", "lesson.module", "lesson.module.course"})
    List<UserProgress> findByUserIdIn(Collection<String> userIds);

    List<UserProgress> findByUserIdAndCourseId(String userId, String courseId);

    Optional<UserProgress> findByUserIdAndLessonId(String userId, String lessonId);
}
