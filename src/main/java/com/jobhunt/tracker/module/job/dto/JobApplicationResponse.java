package com.jobhunt.tracker.module.job.dto;

import com.jobhunt.tracker.module.job.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record JobApplicationResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String position,
        String jobUrl,
        String jobDescription,
        LocalDate appliedDate,
        JobSource source,
        JobStatus status,
        JobPriority priority,
        Long salaryMin,
        Long salaryMax,
        String currency,
        Boolean isRemote,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}