package com.example.analytics.repository;

import com.example.analytics.model.CourseStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseStatRepository extends JpaRepository<CourseStat, String> {
}
