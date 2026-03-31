package com.jobhunt.tracker.module.stats.dto;

import java.util.Map;

public record OfferStatsResponse(
        long total,
        Map<String, Long> byDecision,
        double offerRate
) {
}