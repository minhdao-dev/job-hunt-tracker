package com.jobhunt.tracker.module.stats.service;

import com.jobhunt.tracker.module.interview.repository.InterviewRepository;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.offer.repository.OfferRepository;
import com.jobhunt.tracker.module.reminder.repository.ReminderRepository;
import com.jobhunt.tracker.module.stats.dto.InterviewStatsResponse;
import com.jobhunt.tracker.module.stats.dto.JobStatsResponse;
import com.jobhunt.tracker.module.stats.dto.OfferStatsResponse;
import com.jobhunt.tracker.module.stats.dto.OverviewStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StatsServiceImpl Tests")
class StatsServiceImplTest {

    @Mock
    private JobApplicationRepository jobRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private OfferRepository offerRepository;
    @Mock
    private ReminderRepository reminderRepository;

    @InjectMocks
    private StatsServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        given(jobRepository.countByStatus(userId)).willReturn(rows(
                new Object[]{"APPLIED", 5L},
                new Object[]{"INTERVIEWING", 3L},
                new Object[]{"OFFERED", 1L},
                new Object[]{"REJECTED", 2L},
                new Object[]{"GHOSTED", 1L}
        ));
        given(jobRepository.countBySource(userId)).willReturn(rows(
                new Object[]{"LINKEDIN", 6L},
                new Object[]{"ITVIEC", 4L},
                new Object[]{"OTHER", 2L}
        ));
        given(jobRepository.countByPriority(userId)).willReturn(rows(
                new Object[]{"HIGH", 4L},
                new Object[]{"MEDIUM", 6L},
                new Object[]{"LOW", 2L}
        ));
        given(jobRepository.countActiveJobs(userId)).willReturn(9L);
        given(interviewRepository.countByResult(userId)).willReturn(rows(
                new Object[]{"PASSED", 2L},
                new Object[]{"FAILED", 1L},
                new Object[]{"PENDING", 1L}
        ));
        given(interviewRepository.countByType(userId)).willReturn(rows(
                new Object[]{"TECHNICAL", 2L},
                new Object[]{"HR", 1L},
                new Object[]{"PHONE_SCREENING", 1L}
        ));
        given(interviewRepository.countDistinctJobsWithInterview(userId)).willReturn(4L);
        given(offerRepository.countByDecision(userId)).willReturn(rows(
                new Object[]{"PENDING", 1L}
        ));
        given(reminderRepository.countPendingByUserId(userId)).willReturn(3L);
    }

    private List<Object[]> rows(Object[]... data) {
        return Arrays.asList(data);
    }

    @Nested
    @DisplayName("getOverview()")
    class GetOverviewTests {

        @Test
        @DisplayName("Happy path: trả về đủ các field overview")
        void getOverview_success_returnsAllFields() {
            OverviewStatsResponse result = service.getOverview(userId);

            assertThat(result).isNotNull();
            assertThat(result.totalJobs()).isEqualTo(12L);
            assertThat(result.activeJobs()).isEqualTo(9L);
            assertThat(result.totalInterviews()).isEqualTo(4L);
            assertThat(result.totalOffers()).isEqualTo(1L);
            assertThat(result.pendingReminders()).isEqualTo(3L);
        }

        @Test
        @DisplayName("responseRate = (jobsWithInterview / totalJobs) * 100, làm tròn 1 chữ số")
        void getOverview_calculatesResponseRateCorrectly() {
            OverviewStatsResponse result = service.getOverview(userId);

            // 4 / 12 * 100 = 33.3%
            assertThat(result.responseRate()).isEqualTo(33.3);
        }

        @Test
        @DisplayName("offerRate = (totalOffers / totalJobs) * 100, làm tròn 1 chữ số")
        void getOverview_calculatesOfferRateCorrectly() {
            OverviewStatsResponse result = service.getOverview(userId);

            // 1 / 12 * 100 = 8.3%
            assertThat(result.offerRate()).isEqualTo(8.3);
        }

        @Test
        @DisplayName("totalJobs = 0 → responseRate = 0.0, không throw divide by zero")
        void getOverview_noJobs_responseRateIsZero() {
            given(jobRepository.countByStatus(userId)).willReturn(List.of());

            OverviewStatsResponse result = service.getOverview(userId);

            assertThat(result.totalJobs()).isZero();
            assertThat(result.responseRate()).isEqualTo(0.0);
            assertThat(result.offerRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("totalJobs = 0 → offerRate = 0.0, không throw divide by zero")
        void getOverview_noJobs_offerRateIsZero() {
            given(jobRepository.countByStatus(userId)).willReturn(List.of());
            given(offerRepository.countByDecision(userId)).willReturn(List.of());

            OverviewStatsResponse result = service.getOverview(userId);

            assertThat(result.offerRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Không có reminder → pendingReminders = 0")
        void getOverview_noReminders_pendingIsZero() {
            given(reminderRepository.countPendingByUserId(userId)).willReturn(0L);

            OverviewStatsResponse result = service.getOverview(userId);

            assertThat(result.pendingReminders()).isZero();
        }
    }

    @Nested
    @DisplayName("getJobStats()")
    class GetJobStatsTests {

        @Test
        @DisplayName("Happy path: total = tổng tất cả status")
        void getJobStats_success_totalIsCorrect() {
            JobStatsResponse result = service.getJobStats(userId);

            assertThat(result.total()).isEqualTo(12L);
        }

        @Test
        @DisplayName("byStatus map đúng key-value từ query")
        void getJobStats_byStatusMapIsCorrect() {
            JobStatsResponse result = service.getJobStats(userId);

            assertThat(result.byStatus()).containsEntry("APPLIED", 5L);
            assertThat(result.byStatus()).containsEntry("INTERVIEWING", 3L);
            assertThat(result.byStatus()).containsEntry("OFFERED", 1L);
            assertThat(result.byStatus()).containsEntry("REJECTED", 2L);
            assertThat(result.byStatus()).containsEntry("GHOSTED", 1L);
        }

        @Test
        @DisplayName("bySource map đúng key-value từ query")
        void getJobStats_bySourceMapIsCorrect() {
            JobStatsResponse result = service.getJobStats(userId);

            assertThat(result.bySource()).containsEntry("LINKEDIN", 6L);
            assertThat(result.bySource()).containsEntry("ITVIEC", 4L);
            assertThat(result.bySource()).containsEntry("OTHER", 2L);
        }

        @Test
        @DisplayName("byPriority map đúng key-value từ query")
        void getJobStats_byPriorityMapIsCorrect() {
            JobStatsResponse result = service.getJobStats(userId);

            assertThat(result.byPriority()).containsEntry("HIGH", 4L);
            assertThat(result.byPriority()).containsEntry("MEDIUM", 6L);
            assertThat(result.byPriority()).containsEntry("LOW", 2L);
        }

        @Test
        @DisplayName("Không có job nào → total = 0, các map rỗng")
        void getJobStats_noJobs_returnsZeroAndEmptyMaps() {
            given(jobRepository.countByStatus(userId)).willReturn(List.of());
            given(jobRepository.countBySource(userId)).willReturn(List.of());
            given(jobRepository.countByPriority(userId)).willReturn(List.of());

            JobStatsResponse result = service.getJobStats(userId);

            assertThat(result.total()).isZero();
            assertThat(result.byStatus()).isEmpty();
            assertThat(result.bySource()).isEmpty();
            assertThat(result.byPriority()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getInterviewStats()")
    class GetInterviewStatsTests {

        @Test
        @DisplayName("Happy path: total = tổng tất cả result")
        void getInterviewStats_success_totalIsCorrect() {
            InterviewStatsResponse result = service.getInterviewStats(userId);

            assertThat(result.total()).isEqualTo(4L);
        }

        @Test
        @DisplayName("byResult map đúng key-value từ query")
        void getInterviewStats_byResultMapIsCorrect() {
            InterviewStatsResponse result = service.getInterviewStats(userId);

            assertThat(result.byResult()).containsEntry("PASSED", 2L);
            assertThat(result.byResult()).containsEntry("FAILED", 1L);
            assertThat(result.byResult()).containsEntry("PENDING", 1L);
        }

        @Test
        @DisplayName("byType map đúng key-value từ query")
        void getInterviewStats_byTypeMapIsCorrect() {
            InterviewStatsResponse result = service.getInterviewStats(userId);

            assertThat(result.byType()).containsEntry("TECHNICAL", 2L);
            assertThat(result.byType()).containsEntry("HR", 1L);
            assertThat(result.byType()).containsEntry("PHONE_SCREENING", 1L);
        }

        @Test
        @DisplayName("responseRate tính đúng = jobsWithInterview / totalJobs * 100")
        void getInterviewStats_responseRateIsCorrect() {
            InterviewStatsResponse result = service.getInterviewStats(userId);

            // 4 / 12 * 100 = 33.3%
            assertThat(result.responseRate()).isEqualTo(33.3);
        }

        @Test
        @DisplayName("totalJobs = 0 → responseRate = 0.0")
        void getInterviewStats_noJobs_responseRateIsZero() {
            given(jobRepository.countByStatus(userId)).willReturn(List.of());

            InterviewStatsResponse result = service.getInterviewStats(userId);

            assertThat(result.responseRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Không có interview → total = 0, các map rỗng")
        void getInterviewStats_noInterviews_returnsZeroAndEmptyMaps() {
            given(interviewRepository.countByResult(userId)).willReturn(List.of());
            given(interviewRepository.countByType(userId)).willReturn(List.of());
            given(interviewRepository.countDistinctJobsWithInterview(userId)).willReturn(0L);

            InterviewStatsResponse result = service.getInterviewStats(userId);

            assertThat(result.total()).isZero();
            assertThat(result.byResult()).isEmpty();
            assertThat(result.byType()).isEmpty();
            assertThat(result.responseRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getOfferStats()")
    class GetOfferStatsTests {

        @Test
        @DisplayName("Happy path: total = tổng tất cả decision")
        void getOfferStats_success_totalIsCorrect() {
            OfferStatsResponse result = service.getOfferStats(userId);

            assertThat(result.total()).isEqualTo(1L);
        }

        @Test
        @DisplayName("byDecision map đúng key-value từ query")
        void getOfferStats_byDecisionMapIsCorrect() {
            OfferStatsResponse result = service.getOfferStats(userId);

            assertThat(result.byDecision()).containsEntry("PENDING", 1L);
        }

        @Test
        @DisplayName("offerRate = (totalOffers / totalJobs) * 100, làm tròn 1 chữ số")
        void getOfferStats_offerRateIsCorrect() {
            OfferStatsResponse result = service.getOfferStats(userId);

            // 1 / 12 * 100 = 8.3%
            assertThat(result.offerRate()).isEqualTo(8.3);
        }

        @Test
        @DisplayName("totalJobs = 0 → offerRate = 0.0")
        void getOfferStats_noJobs_offerRateIsZero() {
            given(jobRepository.countByStatus(userId)).willReturn(List.of());

            OfferStatsResponse result = service.getOfferStats(userId);

            assertThat(result.offerRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Không có offer → total = 0, byDecision rỗng, offerRate = 0.0")
        void getOfferStats_noOffers_returnsZeroAndEmptyMap() {
            given(offerRepository.countByDecision(userId)).willReturn(List.of());

            OfferStatsResponse result = service.getOfferStats(userId);

            assertThat(result.total()).isZero();
            assertThat(result.byDecision()).isEmpty();
            assertThat(result.offerRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Nhiều decision → map chứa đủ các key")
        void getOfferStats_multipleDecisions_mapContainsAllKeys() {
            given(offerRepository.countByDecision(userId)).willReturn(List.of(
                    new Object[]{"ACCEPTED", 2L},
                    new Object[]{"REJECTED", 1L},
                    new Object[]{"NEGOTIATING", 1L},
                    new Object[]{"PENDING", 1L}
            ));

            OfferStatsResponse result = service.getOfferStats(userId);

            assertThat(result.total()).isEqualTo(5L);
            assertThat(result.byDecision()).hasSize(4);
            assertThat(result.byDecision()).containsEntry("ACCEPTED", 2L);
            assertThat(result.byDecision()).containsEntry("REJECTED", 1L);
            assertThat(result.byDecision()).containsEntry("NEGOTIATING", 1L);
            assertThat(result.byDecision()).containsEntry("PENDING", 1L);
        }
    }

    @Nested
    @DisplayName("round() — làm tròn 1 chữ số thập phân")
    class RoundTests {

        @Test
        @DisplayName("33.33... → 33.3")
        void round_repeatingDecimal_roundsCorrectly() {
            given(interviewRepository.countDistinctJobsWithInterview(userId)).willReturn(4L);

            OverviewStatsResponse result = service.getOverview(userId);

            assertThat(result.responseRate()).isEqualTo(33.3);
        }

        @Test
        @DisplayName("100% → 100.0")
        void round_perfectRate_returnsHundred() {
            given(interviewRepository.countDistinctJobsWithInterview(userId)).willReturn(12L);

            OverviewStatsResponse result = service.getOverview(userId);

            assertThat(result.responseRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("0% → 0.0")
        void round_zeroRate_returnsZero() {
            given(interviewRepository.countDistinctJobsWithInterview(userId)).willReturn(0L);

            OverviewStatsResponse result = service.getOverview(userId);

            assertThat(result.responseRate()).isEqualTo(0.0);
        }
    }
}