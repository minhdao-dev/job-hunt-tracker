package com.jobhunt.tracker.module.user.dto;

import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String fullName,
        String email,
        String avatarUrl,
        String bio
) {
}