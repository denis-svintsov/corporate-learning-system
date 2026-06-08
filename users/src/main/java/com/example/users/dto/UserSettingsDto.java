package com.example.users.dto;

public record UserSettingsDto(
        String userId,
        boolean emailNotifications,
        boolean pushNotifications,
        String language,
        String timezone
) {
}
