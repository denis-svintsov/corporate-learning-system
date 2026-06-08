package com.example.analytics.repository;

import com.example.analytics.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserActivityRepository extends JpaRepository<UserActivity, String> {
    Optional<UserActivity> findByUserIdAndActivityDate(String userId, LocalDate activityDate);
}
