package com.jobhunt.tracker.module.reminder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateReminderRequest(

        @NotNull(message = "Remind time is required")
        LocalDateTime remindAt,

        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        String message
) {
}