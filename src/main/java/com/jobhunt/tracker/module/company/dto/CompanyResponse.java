package com.jobhunt.tracker.module.company.dto;

import com.jobhunt.tracker.module.company.entity.CompanySize;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String website,
        String industry,
        CompanySize size,
        String location,
        Boolean isOutsource,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}