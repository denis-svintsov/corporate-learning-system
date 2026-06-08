package org.example.courses.repository;

import org.example.courses.model.Lesson;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, String> {

    @EntityGraph(attributePaths = {"module", "module.course"})
    List<Lesson> findByModuleCourseId(String courseId);

    @EntityGraph(attributePaths = {"module", "module.course"})
    List<Lesson> findByModuleCourseIdIn(Collection<String> courseIds);
}
