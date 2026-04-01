package com.jobhunt.tracker.module.job.dto;

import com.jobhunt.tracker.module.job.entity.JobStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(

        @NotNull(message = "Status is required")
        JobStatus status,

        String note
) {
}