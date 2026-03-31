package com.jobhunt.tracker.module.interview.dto;

import com.jobhunt.tracker.module.interview.entity.InterviewResult;
import com.jobhunt.tracker.module.interview.entity.InterviewType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterviewResponse(
        UUID id,
        UUID jobId,
        UUID contactId,
        Integer round,
        InterviewType interviewType,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        String location,
        InterviewResult result,
        String preparationNote,
        String questionsAsked,
        String myAnswers,
        String feedback,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}