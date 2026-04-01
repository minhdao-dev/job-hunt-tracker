package com.jobhunt.tracker.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(

        @NotBlank(message = "New email is required")
        @Email(message = "Invalid email format")
        String newEmail,

        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}