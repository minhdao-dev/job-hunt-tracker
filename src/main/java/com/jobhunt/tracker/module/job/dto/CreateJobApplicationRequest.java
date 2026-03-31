package com.jobhunt.tracker.module.job.dto;

import com.jobhunt.tracker.module.job.entity.JobPriority;
import com.jobhunt.tracker.module.job.entity.JobSource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateJobApplicationRequest(

        UUID companyId,

        @NotBlank(message = "Position is required")
        @Size(max = 255, message = "Position must not exceed 255 characters")
        String position,

        @Size(max = 500, message = "Job URL must not exceed 500 characters")
        String jobUrl,

        String jobDescription,

        LocalDate appliedDate,

        JobSource source,

        JobPriority priority,

        @Min(value = 0, message = "Salary min must be non-negative")
        Long salaryMin,

        @Min(value = 0, message = "Salary max must be non-negative")
        Long salaryMax,

        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,

        Boolean isRemote,

        String notes
) {
}