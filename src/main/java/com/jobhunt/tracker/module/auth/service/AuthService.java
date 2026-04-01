package com.jobhunt.tracker.module.auth.service;

import com.jobhunt.tracker.module.auth.dto.*;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken, String accessToken);

    void logoutAll(String refreshToken, String accessToken);

    void verifyEmail(String token);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(String email, ChangePasswordRequest request, String accessToken);

    void resendVerificationEmail(String email);
}