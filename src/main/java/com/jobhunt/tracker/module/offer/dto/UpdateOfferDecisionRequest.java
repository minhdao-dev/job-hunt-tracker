package com.jobhunt.tracker.module.offer.dto;

import com.jobhunt.tracker.module.offer.entity.OfferDecision;
import jakarta.validation.constraints.NotNull;

public record UpdateOfferDecisionRequest(

        @NotNull(message = "Decision is required")
        OfferDecision decision,

        String note
) {
}