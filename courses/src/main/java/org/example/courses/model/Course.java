package org.example.courses.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course", schema = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private DifficultyLevel difficulty;

    /**
     * Общая длительность в минутах.
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CourseStatus status;

    /**
     * Ограничение доступа по ролям. Хранится как CSV: "HR,ADMIN,EMPLOYEE".
     * Если null/пусто — доступно всем.
     */
    @Column(name = "allowed_roles_csv")
    private String allowedRolesCsv;

    /**
     * Ограничение доступа по департаментам. Хранится как CSV department_id.
     * Если null/пусто — доступно всем.
     */
    @Column(name = "allowed_department_ids_csv")
    private String allowedDepartmentIdsCsv;

    /**
     * Целевые должности курса, хранятся в CSV (legacy column name: specialization).
     */
    @Column(name = "specialization")
    private String specializationsCsv;

    /**
     * Инструкции по прохождению курса.
     */
    @Column(name = "instructions", columnDefinition = "text")
    private String instructions;

    /**
     * Ссылка на внешний агрегатор, где проходит обучение.
     */
    @Column(name = "aggregator_url")
    private String aggregatorUrl;

    /**
     * Обложка курса.
     */
    @Column(name = "cover_url")
    private String coverUrl;

    /**
     * Стоимость курса для компании (видна администраторам).
     */
    @Column(name = "company_cost")
    private BigDecimal companyCost;

    /**
     * Партнер, на площадке которого проводится курс.
     */
    @Column(name = "partner_name")
    private String partnerName;

    /**
     * Локация/площадка партнера.
     */
    @Column(name = "partner_location")
    private String partnerLocation;

    /**
     * Фиксированная дата старта потока курса.
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * Фиксированная дата окончания потока курса.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToMany
    @JoinTable(
            name = "course_tag",
            schema = "courses",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = CourseStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Duration getDuration() {
        return durationMinutes != null ? Duration.ofMinutes(durationMinutes) : null;
    }
}
