package com.jobhunt.tracker.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateNotificationRequest(

        @NotNull(message = "reminderEnabled is required")
        Boolean reminderEnabled,

        @NotNull(message = "reminderAfterDays is required")
        @Min(value = 1, message = "Reminder days must be at least 1")
        @Max(value = 30, message = "Reminder days must not exceed 30")
        Integer reminderAfterDays,

        @NotNull(message = "emailNotifications is required")
        Boolean emailNotifications,

        @Size(max = 50, message = "Timezone must not exceed 50 characters")
        String timezone,

        @Size(max = 10, message = "Language must not exceed 10 characters")
        String language
) {
}