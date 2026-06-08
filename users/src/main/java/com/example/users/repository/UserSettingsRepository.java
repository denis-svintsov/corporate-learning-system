package com.example.users.repository;

import com.example.users.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, String> {
}
