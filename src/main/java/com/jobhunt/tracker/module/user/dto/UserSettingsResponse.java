package com.jobhunt.tracker.module.user.dto;

public record UserSettingsResponse(
        ProfileResponse profile,
        NotificationResponse notification,
        PreferencesResponse preferences
) {
}