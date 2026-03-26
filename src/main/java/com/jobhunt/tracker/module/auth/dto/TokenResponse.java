package com.jobhunt.tracker.module.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn
) {
    public static TokenResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn) {

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn
        );
    }
}