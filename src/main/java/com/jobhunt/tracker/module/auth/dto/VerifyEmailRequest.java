package com.jobhunt.tracker.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "Token is required")
        String token
) {}