package com.jobhunt.tracker.module.stats.service;

import com.jobhunt.tracker.module.stats.dto.*;

import java.util.UUID;

public interface StatsService {

    OverviewStatsResponse getOverview(UUID userId);

    JobStatsResponse getJobStats(UUID userId);

    InterviewStatsResponse getInterviewStats(UUID userId);

    OfferStatsResponse getOfferStats(UUID userId);
}