package com.example.users.service;

import com.example.users.dto.UpdateUserSettingsRequest;
import com.example.users.dto.UserSettingsDto;
import com.example.users.model.UserSettings;
import com.example.users.repository.UserRepository;
import com.example.users.repository.UserSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    public UserSettingsService(UserSettingsRepository userSettingsRepository, UserRepository userRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.userRepository = userRepository;
    }

    public UserSettingsDto getByUserId(String userId) {
        ensureUserExists(userId);
        return toDto(userSettingsRepository.findById(userId).orElseGet(() -> defaults(userId)));
    }

    @Transactional
    public UserSettingsDto update(String userId, UpdateUserSettingsRequest request) {
        ensureUserExists(userId);
        UserSettings settings = userSettingsRepository.findById(userId).orElseGet(() -> defaults(userId));

        if (request.emailNotifications() != null) {
            settings.setEmailNotifications(request.emailNotifications());
        }
        if (request.pushNotifications() != null) {
            settings.setPushNotifications(request.pushNotifications());
        }
        if (request.language() != null && !request.language().isBlank()) {
            settings.setLanguage(request.language());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            settings.setTimezone(request.timezone());
        }

        return toDto(userSettingsRepository.save(settings));
    }

    private void ensureUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
    }

    private UserSettings defaults(String userId) {
        UserSettings settings = new UserSettings();
        settings.setUserId(userId);
        settings.setEmailNotifications(Boolean.TRUE);
        settings.setPushNotifications(Boolean.TRUE);
        settings.setLanguage("ru");
        settings.setTimezone("Europe/Moscow");
        return settings;
    }

    private UserSettingsDto toDto(UserSettings settings) {
        return new UserSettingsDto(
                settings.getUserId(),
                Boolean.TRUE.equals(settings.getEmailNotifications()),
                Boolean.TRUE.equals(settings.getPushNotifications()),
                settings.getLanguage(),
                settings.getTimezone()
        );
    }
}
