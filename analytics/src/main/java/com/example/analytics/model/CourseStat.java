package com.example.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "course_stat", schema = "analytics")
public class CourseStat {

    @Id
    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(nullable = false)
    private long assignments;

    @Column(nullable = false)
    private long completions;

    @Column(name = "lessons_completed", nullable = false)
    private long lessonsCompleted;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CourseStat() {
    }

    public CourseStat(String courseId) {
        this.courseId = courseId;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getCourseId() {
        return courseId;
    }

    public long getAssignments() {
        return assignments;
    }

    public long getCompletions() {
        return completions;
    }

    public long getLessonsCompleted() {
        return lessonsCompleted;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void addAssignment() {
        assignments++;
        touch();
    }

    public void addCompletion() {
        completions++;
        touch();
    }

    public void addLessonCompleted() {
        lessonsCompleted++;
        touch();
    }

    private void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
