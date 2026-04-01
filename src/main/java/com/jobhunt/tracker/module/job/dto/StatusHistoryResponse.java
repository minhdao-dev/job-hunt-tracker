package com.jobhunt.tracker.module.job.dto;

import com.jobhunt.tracker.module.job.entity.JobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusHistoryResponse(
        UUID id,
        JobStatus oldStatus,
        JobStatus newStatus,
        String note,
        LocalDateTime changedAt
) {
}