package com.jobhunt.tracker.module.stats.dto;

import java.util.Map;

public record JobStatsResponse(
        long total,
        Map<String, Long> byStatus,
        Map<String, Long> bySource,
        Map<String, Long> byPriority
) {
}