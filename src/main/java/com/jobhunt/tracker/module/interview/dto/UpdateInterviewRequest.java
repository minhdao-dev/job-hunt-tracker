package com.jobhunt.tracker.module.interview.dto;

import com.jobhunt.tracker.module.interview.entity.InterviewType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateInterviewRequest(

        UUID contactId,

        @Min(value = 1, message = "Round must be at least 1")
        Integer round,

        InterviewType interviewType,

        @NotNull(message = "Scheduled time is required")
        LocalDateTime scheduledAt,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        @Size(max = 500, message = "Location must not exceed 500 characters")
        String location,

        String preparationNote,
        String questionsAsked,
        String myAnswers,
        String feedback
) {
}