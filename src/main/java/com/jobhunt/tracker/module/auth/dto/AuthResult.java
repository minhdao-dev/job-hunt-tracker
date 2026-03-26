package com.jobhunt.tracker.module.auth.dto;

public record AuthResult(
        AuthResponse authResponse,
        String refreshToken
) {
}