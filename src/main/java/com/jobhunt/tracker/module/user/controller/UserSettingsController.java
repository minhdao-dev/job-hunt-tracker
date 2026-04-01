package com.jobhunt.tracker.module.user.controller;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.common.response.AppResponse;
import com.jobhunt.tracker.config.security.CookieService;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.user.dto.*;
import com.jobhunt.tracker.module.user.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "User Settings", description = "User profile and settings endpoints")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;
    private final JwtService jwtService;
    private final CookieService cookieService;

    @GetMapping("/settings")
    @Operation(summary = "Get all user settings")
    public ResponseEntity<AppResponse<UserSettingsResponse>> getSettings(
            HttpServletRequest request) {

        UUID userId = extractUserId(request);
        return ResponseEntity.ok(
                AppResponse.success(userSettingsService.getSettings(userId))
        );
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update profile")
    public ResponseEntity<AppResponse<Void>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest body,
            HttpServletRequest request) {

        userSettingsService.updateProfile(extractUserId(request), body);
        return ResponseEntity.ok(AppResponse.success("Profile updated successfully"));
    }

    @PatchMapping("/notification")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<AppResponse<Void>> updateNotification(
            @Valid @RequestBody UpdateNotificationRequest body,
            HttpServletRequest request) {

        userSettingsService.updateNotification(extractUserId(request), body);
        return ResponseEntity.ok(AppResponse.success("Notification settings updated successfully"));
    }

    @PatchMapping("/preferences")
    @Operation(summary = "Update job preferences")
    public ResponseEntity<AppResponse<Void>> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest body,
            HttpServletRequest request) {

        userSettingsService.updatePreferences(extractUserId(request), body);
        return ResponseEntity.ok(AppResponse.success("Job preferences updated successfully"));
    }

    @PostMapping("/change-email")
    @Operation(summary = "Request email change")
    public ResponseEntity<AppResponse<Void>> changeEmail(
            @Valid @RequestBody ChangeEmailRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {

        userSettingsService.changeEmail(
                extractUserId(request), body, extractBearerToken(request)
        );
        cookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(AppResponse.success(
                "Verification email sent to new address. Please verify to complete the change."
        ));
    }

    @DeleteMapping
    @Operation(summary = "Delete account (soft delete)")
    public ResponseEntity<AppResponse<Void>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {

        userSettingsService.deleteAccount(
                extractUserId(request), body, extractBearerToken(request)
        );
        cookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(AppResponse.success("Account deleted successfully."));
    }

    private UUID extractUserId(HttpServletRequest request) {
        return jwtService.extractUserId(extractBearerToken(request));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw AppException.unauthorized("Access token not found");
    }
}