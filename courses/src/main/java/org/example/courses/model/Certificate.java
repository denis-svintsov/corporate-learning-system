package org.example.courses.model;

import jakarta.persistence.*;
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
@Table(name = "certificate", schema = "courses")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "issue_date", nullable = false)
    private OffsetDateTime issueDate;

    @Column(name = "certificate_url", nullable = false)
    private String certificateUrl;

    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "hash", nullable = false, unique = true)
    private String hash;

    @PrePersist
    public void prePersist() {
        if (issueDate == null) {
            issueDate = OffsetDateTime.now();
        }
    }
}
