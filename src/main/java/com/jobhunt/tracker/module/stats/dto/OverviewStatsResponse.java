package com.jobhunt.tracker.module.stats.dto;

public record OverviewStatsResponse(
        long totalJobs,
        long activeJobs,
        long totalInterviews,
        long totalOffers,
        long pendingReminders,
        double responseRate,
        double offerRate
) {
}