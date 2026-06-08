package com.example.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_report", schema = "analytics")
public class LearningReport {

    @Id
    private String id;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "generated_by")
    private String generatedBy;

    @Column(nullable = false)
    private String format;

    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    protected LearningReport() {
    }

    public LearningReport(String reportType, String generatedBy, String format, String payloadJson) {
        this.reportType = reportType;
        this.generatedBy = generatedBy;
        this.format = format;
        this.payloadJson = payloadJson;
        this.generatedAt = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getReportType() {
        return reportType;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public String getFormat() {
        return format;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    @PrePersist
    void ensureId() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }
}
