package com.example.analytics.repository;

import com.example.analytics.model.LearningReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningReportRepository extends JpaRepository<LearningReport, String> {
}
