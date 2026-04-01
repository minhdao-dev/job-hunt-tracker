package com.jobhunt.tracker.module.user.dto;

import com.jobhunt.tracker.module.user.entity.WorkType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdatePreferencesRequest(

        @Size(max = 100, message = "Target role must not exceed 100 characters")
        String targetRole,

        @Min(value = 0, message = "Salary min must be non-negative")
        Integer targetSalaryMin,

        @Min(value = 0, message = "Salary max must be non-negative")
        Integer targetSalaryMax,

        @Size(max = 200, message = "Preferred location must not exceed 200 characters")
        String preferredLocation,

        WorkType workType
) {}