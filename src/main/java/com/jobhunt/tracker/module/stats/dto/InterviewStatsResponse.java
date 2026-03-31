package com.jobhunt.tracker.module.stats.dto;

import java.util.Map;

public record InterviewStatsResponse(
        long total,
        Map<String, Long> byResult,
        Map<String, Long> byType,
        double responseRate
) {
}