package com.jobhunt.tracker.module.offer.dto;

import com.jobhunt.tracker.module.offer.entity.OfferDecision;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID jobId,
        Long salary,
        String currency,
        String benefits,
        LocalDate startDate,
        LocalDate expiredAt,
        OfferDecision decision,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}