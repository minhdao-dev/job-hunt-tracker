package com.jobhunt.tracker.module.user.service;

import com.jobhunt.tracker.module.user.dto.*;

import java.util.UUID;

public interface UserSettingsService {

    UserSettingsResponse getSettings(UUID userId);

    void updateProfile(UUID userId, UpdateProfileRequest request);

    void updateNotification(UUID userId, UpdateNotificationRequest request);

    void updatePreferences(UUID userId, UpdatePreferencesRequest request);

    void changeEmail(UUID userId, ChangeEmailRequest request, String accessToken);

    void deleteAccount(UUID userId, DeleteAccountRequest request, String accessToken);
}