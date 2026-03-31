package com.jobhunt.tracker.module.interview.dto;

import com.jobhunt.tracker.module.interview.entity.InterviewResult;
import jakarta.validation.constraints.NotNull;

public record UpdateInterviewResultRequest(

        @NotNull(message = "Result is required")
        InterviewResult result,

        String questionsAsked,
        String myAnswers,
        String feedback
) {
}