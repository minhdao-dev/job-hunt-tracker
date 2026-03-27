package com.jobhunt.tracker.module.auth.controller;

import com.jobhunt.tracker.common.response.AppResponse;
import com.jobhunt.tracker.config.security.CookieService;
import com.jobhunt.tracker.module.auth.dto.*;
import com.jobhunt.tracker.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    @PostMapping("/register")
    @Operation(summary = "Register new account")
    public ResponseEntity<AppResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResult result = authService.register(request);

        cookieService.setRefreshTokenCookie(response, result.refreshToken());

        return ResponseEntity
                .status(201)
                .body(AppResponse.created(
                        "Registration successful",
                        result.authResponse()
                ));
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public ResponseEntity<AppResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResult result = authService.login(request);

        cookieService.setRefreshTokenCookie(response, result.refreshToken());

        return ResponseEntity.ok(
                AppResponse.success("Login successful", result.authResponse())
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AppResponse<TokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = cookieService.getRefreshToken(request)
                .orElseThrow(() ->
                        com.jobhunt.tracker.common.exception.AppException
                                .unauthorized("Refresh token not found")
                );

        TokenResponse tokenResponse = authService.refresh(refreshToken);

        cookieService.setRefreshTokenCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(
                AppResponse.success("Token refreshed", tokenResponse)
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current device")
    public ResponseEntity<AppResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String accessToken = extractBearerToken(request);

        cookieService.getRefreshToken(request)
                .ifPresent(rt -> authService.logout(rt, accessToken));

        cookieService.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(
                AppResponse.success("Logged out successfully")
        );
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all devices")
    public ResponseEntity<AppResponse<Void>> logoutAll(
            HttpServletRequest request,
            HttpServletResponse response) {

        String accessToken = extractBearerToken(request);

        cookieService.getRefreshToken(request)
                .ifPresent(rt -> authService.logoutAll(rt, accessToken));

        cookieService.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(
                AppResponse.success("Logged out from all devices")
        );
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public ResponseEntity<AppResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        authService.verifyEmail(request.token());

        return ResponseEntity.ok(
                AppResponse.success("Email verified successfully")
        );
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send reset password email")
    public ResponseEntity<AppResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request.email());

        return ResponseEntity.ok(
                AppResponse.success(
                        "If your email is registered, " +
                                "you will receive a reset link shortly"
                )
        );
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<AppResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                AppResponse.success(
                        "Password reset successfully. Please login again."
                )
        );
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password (authenticated)")
    public ResponseEntity<AppResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String accessToken = extractBearerToken(httpRequest);

        authService.changePassword(
                userDetails.getUsername(), request, accessToken
        );

        cookieService.clearRefreshTokenCookie(httpResponse);

        return ResponseEntity.ok(
                AppResponse.success(
                        "Password changed successfully. Please login again."
                )
        );
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<AppResponse<Void>> resendVerification(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.resendVerificationEmail(request.email());

        return ResponseEntity.ok(
                AppResponse.success(
                        "If your email is registered and not verified, " +
                                "a verification email will be sent shortly"
                )
        );
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}