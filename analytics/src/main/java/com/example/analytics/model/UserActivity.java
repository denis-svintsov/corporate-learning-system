package com.example.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_activity",
        schema = "analytics",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_activity_day", columnNames = {"user_id", "activity_date"})
)
public class UserActivity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "lessons_completed", nullable = false)
    private long lessonsCompleted;

    @Column(name = "courses_completed", nullable = false)
    private long coursesCompleted;

    @Column(name = "assignments_received", nullable = false)
    private long assignmentsReceived;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserActivity() {
    }

    public UserActivity(String userId, LocalDate activityDate) {
        this.userId = userId;
        this.activityDate = activityDate;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getUserId() {
        return userId;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public long getLessonsCompleted() {
        return lessonsCompleted;
    }

    public long getCoursesCompleted() {
        return coursesCompleted;
    }

    public long getAssignmentsReceived() {
        return assignmentsReceived;
    }

    public void addLessonCompleted() {
        lessonsCompleted++;
        touch();
    }

    public void addCourseCompleted() {
        coursesCompleted++;
        touch();
    }

    public void addAssignmentReceived() {
        assignmentsReceived++;
        touch();
    }

    @PrePersist
    void ensureId() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }

    private void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
