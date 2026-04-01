package com.jobhunt.tracker.module.auth.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String fullName,
        String accessToken,
        String tokenType,
        long accessTokenExpiresIn
) {
    public static AuthResponse of(
            UUID userId,
            String email,
            String fullName,
            String accessToken,
            long expiresIn) {

        return new AuthResponse(
                userId,
                email,
                fullName,
                accessToken,
                "Bearer",
                expiresIn
        );
    }
}