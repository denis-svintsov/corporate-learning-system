package org.example.courses.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assignment_policy", schema = "courses")
public class AssignmentPolicy {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "max_courses_per_quarter", nullable = false)
    private Integer maxCoursesPerQuarter;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = 1;
        }
        if (maxCoursesPerQuarter == null || maxCoursesPerQuarter < 1) {
            maxCoursesPerQuarter = 3;
        }
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
