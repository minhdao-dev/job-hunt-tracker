package com.jobhunt.tracker.module.reminder.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        UUID jobId,
        String jobPosition,
        String message,
        LocalDateTime remindAt,
        Boolean isSent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}