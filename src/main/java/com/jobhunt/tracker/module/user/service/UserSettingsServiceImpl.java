package com.jobhunt.tracker.module.user.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.config.mail.EmailService;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.config.security.TokenBlacklistService;
import com.jobhunt.tracker.module.auth.entity.OtpToken;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.auth.repository.OtpTokenRepository;
import com.jobhunt.tracker.module.auth.repository.RefreshTokenRepository;
import com.jobhunt.tracker.module.auth.repository.UserRepository;
import com.jobhunt.tracker.module.user.dto.*;
import com.jobhunt.tracker.module.user.entity.UserSettings;
import com.jobhunt.tracker.module.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserRepository         userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository     otpTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtService             jwtService;
    private final TokenBlacklistService  blacklistService;
    private final EmailService           emailService;

    @Override
    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(UUID userId) {
        User user = findActiveUser(userId);
        UserSettings settings = findOrCreateSettings(user);

        return new UserSettingsResponse(
                new ProfileResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getAvatarUrl(),
                        settings.getBio()
                ),
                new NotificationResponse(
                        settings.getReminderEnabled(),
                        settings.getReminderAfterDays(),
                        settings.getEmailNotifications(),
                        settings.getTimezone(),
                        settings.getLanguage()
                ),
                new PreferencesResponse(
                        settings.getTargetRole(),
                        settings.getTargetSalaryMin(),
                        settings.getTargetSalaryMax(),
                        settings.getPreferredLocation(),
                        settings.getWorkType()
                )
        );
    }

    @Override
    @Transactional
    public void updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        UserSettings settings = findOrCreateSettings(user);

        user.setFullName(request.fullName());
        user.setAvatarUrl(request.avatarUrl());
        userRepository.save(user);

        settings.setBio(request.bio());
        userSettingsRepository.save(settings);

        log.info("Profile updated for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void updateNotification(UUID userId, UpdateNotificationRequest request) {
        User user = findActiveUser(userId);
        UserSettings settings = findOrCreateSettings(user);

        settings.setReminderEnabled(request.reminderEnabled());
        settings.setReminderAfterDays(request.reminderAfterDays());
        settings.setEmailNotifications(request.emailNotifications());

        if (request.timezone() != null) settings.setTimezone(request.timezone());
        if (request.language() != null) settings.setLanguage(request.language());

        userSettingsRepository.save(settings);

        log.info("Notification settings updated for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        User user = findActiveUser(userId);
        UserSettings settings = findOrCreateSettings(user);

        if (request.targetSalaryMin() != null
                && request.targetSalaryMax() != null
                && request.targetSalaryMin() > request.targetSalaryMax()) {
            throw AppException.badRequest("Salary min must not exceed salary max");
        }

        settings.setTargetRole(request.targetRole());
        settings.setTargetSalaryMin(request.targetSalaryMin());
        settings.setTargetSalaryMax(request.targetSalaryMax());
        settings.setPreferredLocation(request.preferredLocation());
        settings.setWorkType(request.workType());

        userSettingsRepository.save(settings);

        log.info("Job preferences updated for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changeEmail(UUID userId, ChangeEmailRequest request, String accessToken) {
        User user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw AppException.badRequest("Current password is incorrect");
        }

        if (user.getEmail().equalsIgnoreCase(request.newEmail())) {
            throw AppException.badRequest("New email must be different from current email");
        }

        if (userRepository.existsByEmail(request.newEmail())) {
            throw AppException.conflict("Email already in use: " + request.newEmail());
        }

        user.setPendingEmail(request.newEmail());
        userRepository.save(user);

        otpTokenRepository.invalidateAllByUserIdAndType(
                userId, OtpToken.OtpType.EMAIL_VERIFICATION
        );

        String token = UUID.randomUUID().toString();
        OtpToken otp = OtpToken.builder()
                .user(user)
                .token(token)
                .type(OtpToken.OtpType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        otpTokenRepository.save(otp);

        emailService.sendVerificationEmail(
                request.newEmail(), user.getFullName(), token
        );

        refreshTokenRepository.revokeAllByUserId(userId);
        blacklistCurrentToken(accessToken);

        log.info("Change email requested: {} → {}", user.getEmail(), request.newEmail());
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequest request, String accessToken) {
        User user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw AppException.badRequest("Current password is incorrect");
        }

        refreshTokenRepository.revokeAllByUserId(userId);
        blacklistCurrentToken(accessToken);

        user.softDelete();
        userRepository.save(user);

        log.info("Account soft-deleted for user: {}", user.getEmail());
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private User findActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    private UserSettings findOrCreateSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSettings settings = UserSettings.builder()
                            .user(user)
                            .build();
                    return userSettingsRepository.save(settings);
                });
    }

    private void blacklistCurrentToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return;
        if (!jwtService.isTokenValid(accessToken)) return;
        String jti = jwtService.extractJti(accessToken);
        long ttl = jwtService.getRemainingTtlMillis(accessToken);
        blacklistService.blacklist(jti, ttl);
    }
}