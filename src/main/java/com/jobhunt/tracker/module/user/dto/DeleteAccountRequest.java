package com.jobhunt.tracker.module.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}