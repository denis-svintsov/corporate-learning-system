package com.example.users.dto;

public record UpdateUserSettingsRequest(
        Boolean emailNotifications,
        Boolean pushNotifications,
        String language,
        String timezone
) {
}
