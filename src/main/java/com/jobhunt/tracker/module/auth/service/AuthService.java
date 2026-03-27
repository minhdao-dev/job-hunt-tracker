package com.jobhunt.tracker.module.auth.service;

import com.jobhunt.tracker.module.auth.dto.*;

public interface AuthService {
    AuthResult register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);

    void logoutAll(String refreshToken);

    void verifyEmail(String token);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    void resendVerificationEmail(String email);
}