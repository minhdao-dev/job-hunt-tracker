package com.jobhunt.tracker.module.stats.service;

import com.jobhunt.tracker.module.interview.repository.InterviewRepository;
import com.jobhunt.tracker.module.job.entity.JobStatus;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.offer.repository.OfferRepository;
import com.jobhunt.tracker.module.reminder.repository.ReminderRepository;
import com.jobhunt.tracker.module.stats.dto.InterviewStatsResponse;
import com.jobhunt.tracker.module.stats.dto.JobStatsResponse;
import com.jobhunt.tracker.module.stats.dto.OfferStatsResponse;
import com.jobhunt.tracker.module.stats.dto.OverviewStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final JobApplicationRepository jobRepository;
    private final InterviewRepository interviewRepository;
    private final OfferRepository offerRepository;
    private final ReminderRepository reminderRepository;

    @Override
    @Transactional(readOnly = true)
    public OverviewStatsResponse getOverview(UUID userId) {
        long totalJobs = jobRepository.countByStatus(userId)
                .stream().mapToLong(r -> (Long) r[1]).sum();

        long activeJobs = jobRepository.countActiveJobs(
                userId,
                List.of(JobStatus.APPLIED, JobStatus.INTERVIEWING, JobStatus.OFFERED)
        );

        long totalInterviews = interviewRepository.countByResult(userId)
                .stream().mapToLong(r -> (Long) r[1]).sum();

        long totalOffers = offerRepository.countByDecision(userId)
                .stream().mapToLong(r -> (Long) r[1]).sum();

        long pendingReminders = reminderRepository.countPendingByUserId(userId);

        double responseRate = totalJobs == 0 ? 0.0
                : round((double) interviewRepository.countDistinctJobsWithInterview(userId)
                / totalJobs * 100);

        double offerRate = totalJobs == 0 ? 0.0
                : round((double) totalOffers / totalJobs * 100);

        return new OverviewStatsResponse(
                totalJobs,
                activeJobs,
                totalInterviews,
                totalOffers,
                pendingReminders,
                responseRate,
                offerRate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public JobStatsResponse getJobStats(UUID userId) {
        List<Object[]> statusRows = jobRepository.countByStatus(userId);

        long total = statusRows.stream().mapToLong(r -> (Long) r[1]).sum();
        Map<String, Long> byStatus = toMap(statusRows);
        Map<String, Long> bySource = toMap(jobRepository.countBySource(userId));
        Map<String, Long> byPriority = toMap(jobRepository.countByPriority(userId));

        return new JobStatsResponse(total, byStatus, bySource, byPriority);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewStatsResponse getInterviewStats(UUID userId) {
        List<Object[]> resultRows = interviewRepository.countByResult(userId);  // gọi 1 lần

        long total = resultRows.stream().mapToLong(r -> (Long) r[1]).sum();
        Map<String, Long> byResult = toMap(resultRows);
        Map<String, Long> byType = toMap(interviewRepository.countByType(userId));

        long totalJobs = jobRepository.countByStatus(userId)
                .stream().mapToLong(r -> (Long) r[1]).sum();
        double responseRate = totalJobs == 0 ? 0.0
                : round((double) interviewRepository.countDistinctJobsWithInterview(userId)
                / totalJobs * 100);

        return new InterviewStatsResponse(total, byResult, byType, responseRate);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferStatsResponse getOfferStats(UUID userId) {
        long total = offerRepository.countByDecision(userId)
                .stream().mapToLong(r -> (Long) r[1]).sum();

        Map<String, Long> byDecision = toMap(offerRepository.countByDecision(userId));

        long totalJobs = jobRepository.countByStatus(userId)
                .stream().mapToLong(r -> (Long) r[1]).sum();

        double offerRate = totalJobs == 0 ? 0.0
                : round((double) total / totalJobs * 100);

        return new OfferStatsResponse(total, byDecision, offerRate);
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(row[0].toString(), (Long) row[1]);
        }
        return result;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}