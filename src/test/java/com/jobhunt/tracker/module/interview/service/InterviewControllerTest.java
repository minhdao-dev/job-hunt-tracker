package com.jobhunt.tracker.module.interview.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.interview.dto.*;
import com.jobhunt.tracker.module.interview.entity.Interview;
import com.jobhunt.tracker.module.interview.entity.InterviewResult;
import com.jobhunt.tracker.module.interview.entity.InterviewType;
import com.jobhunt.tracker.module.interview.repository.InterviewRepository;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import com.jobhunt.tracker.module.job.entity.JobStatus;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewServiceImpl Tests")
class InterviewServiceImplTest {

    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private JobApplicationRepository jobRepository;

    @InjectMocks
    private InterviewServiceImpl service;

    private JobApplication mockJob;
    private Interview mockInterview;

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID interviewId = UUID.randomUUID();
    private final LocalDateTime scheduledAt = LocalDateTime.now().plusDays(3);

    @BeforeEach
    void setUp() {
        User mockUser = User.builder()
                .email("dao@example.com")
                .passwordHash("$2a$10$hashed")
                .fullName("Nguyen Minh Dao")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", userId);

        mockJob = JobApplication.builder()
                .user(mockUser)
                .position("Java Backend Developer")
                .appliedDate(LocalDate.now())
                .status(JobStatus.INTERVIEWING)
                .build();
        ReflectionTestUtils.setField(mockJob, "id", jobId);

        mockInterview = Interview.builder()
                .job(mockJob)
                .round(1)
                .interviewType(InterviewType.TECHNICAL)
                .scheduledAt(scheduledAt)
                .durationMinutes(60)
                .location("Google Meet")
                .result(InterviewResult.PENDING)
                .preparationNote("Review Java core")
                .build();
        ReflectionTestUtils.setField(mockInterview, "id", interviewId);
        ReflectionTestUtils.setField(mockInterview, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(mockInterview, "updatedAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        private CreateInterviewRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateInterviewRequest(
                    null, 1, InterviewType.TECHNICAL,
                    scheduledAt, 60, "Google Meet", "Review Java core"
            );
        }

        @Test
        @DisplayName("Happy path: tạo interview thành công → trả về response")
        void create_success_returnsResponse() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(interviewRepository.save(any(Interview.class))).willReturn(mockInterview);

            InterviewResponse result = service.create(userId, jobId, request);

            assertThat(result).isNotNull();
            assertThat(result.jobId()).isEqualTo(jobId);
            assertThat(result.round()).isEqualTo(1);
            assertThat(result.interviewType()).isEqualTo(InterviewType.TECHNICAL);
            assertThat(result.scheduledAt()).isEqualTo(scheduledAt);
            assertThat(result.durationMinutes()).isEqualTo(60);
            assertThat(result.location()).isEqualTo("Google Meet");
            assertThat(result.result()).isEqualTo(InterviewResult.PENDING);
        }

        @Test
        @DisplayName("Tạo thành công → Interview được save với đúng dữ liệu")
        void create_success_savesWithCorrectData() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(interviewRepository.save(any(Interview.class))).willReturn(mockInterview);

            service.create(userId, jobId, request);

            ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
            then(interviewRepository).should().save(captor.capture());

            Interview saved = captor.getValue();
            assertThat(saved.getJob()).isEqualTo(mockJob);
            assertThat(saved.getRound()).isEqualTo(1);
            assertThat(saved.getInterviewType()).isEqualTo(InterviewType.TECHNICAL);
            assertThat(saved.getScheduledAt()).isEqualTo(scheduledAt);
            assertThat(saved.getDurationMinutes()).isEqualTo(60);
            assertThat(saved.getLocation()).isEqualTo("Google Meet");
        }

        @Test
        @DisplayName("round null → default 1")
        void create_nullRound_defaultsToOne() {
            CreateInterviewRequest noRoundRequest = new CreateInterviewRequest(
                    null, null, InterviewType.HR,
                    scheduledAt, null, null, null
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(interviewRepository.save(any(Interview.class))).willReturn(mockInterview);

            service.create(userId, jobId, noRoundRequest);

            ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
            then(interviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getRound()).isEqualTo(1);
        }

        @Test
        @DisplayName("interviewType null → default TECHNICAL")
        void create_nullInterviewType_defaultsToTechnical() {
            CreateInterviewRequest noTypeRequest = new CreateInterviewRequest(
                    null, 1, null, scheduledAt, null, null, null
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(interviewRepository.save(any(Interview.class))).willReturn(mockInterview);

            service.create(userId, jobId, noTypeRequest);

            ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
            then(interviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getInterviewType()).isEqualTo(InterviewType.TECHNICAL);
        }

        @Test
        @DisplayName("durationMinutes null → default 60")
        void create_nullDuration_defaultsToSixty() {
            CreateInterviewRequest noDurationRequest = new CreateInterviewRequest(
                    null, 1, null, scheduledAt, null, null, null
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(interviewRepository.save(any(Interview.class))).willReturn(mockInterview);

            service.create(userId, jobId, noDurationRequest);

            ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
            then(interviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getDurationMinutes()).isEqualTo(60);
        }

        @Test
        @DisplayName("contactId có giá trị → được lưu vào Interview")
        void create_withContactId_savesContactId() {
            UUID contactId = UUID.randomUUID();
            CreateInterviewRequest withContactRequest = new CreateInterviewRequest(
                    contactId, 1, InterviewType.HR, scheduledAt, 30, null, null
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(interviewRepository.save(any(Interview.class))).willReturn(mockInterview);

            service.create(userId, jobId, withContactRequest);

            ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
            then(interviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getContactId()).isEqualTo(contactId);
        }

        @Test
        @DisplayName("Job không tồn tại → ném NotFoundException (404)")
        void create_jobNotFound_throwsNotFound() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(userId, jobId, request))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );

            then(interviewRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Job thuộc user khác → ném NotFoundException")
        void create_jobBelongsToOtherUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(jobRepository.findByIdAndUserId(jobId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(otherUserId, jobId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(interviewRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Happy path: trả về danh sách interview của job theo scheduledAt ASC")
        void getAll_success_returnsOrderedList() {
            Interview i2 = Interview.builder()
                    .job(mockJob)
                    .round(2)
                    .interviewType(InterviewType.HR)
                    .scheduledAt(scheduledAt.plusDays(7))
                    .durationMinutes(30)
                    .result(InterviewResult.PENDING)
                    .build();
            ReflectionTestUtils.setField(i2, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(i2, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(i2, "updatedAt", LocalDateTime.now());

            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(interviewRepository.findAllByJobIdAndUserId(jobId, userId))
                    .willReturn(List.of(mockInterview, i2));

            List<InterviewResponse> result = service.getAll(userId, jobId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).round()).isEqualTo(1);
            assertThat(result.get(1).round()).isEqualTo(2);
        }

        @Test
        @DisplayName("Chưa có interview nào → trả về list rỗng")
        void getAll_noInterviews_returnsEmptyList() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(interviewRepository.findAllByJobIdAndUserId(jobId, userId))
                    .willReturn(List.of());

            List<InterviewResponse> result = service.getAll(userId, jobId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Job không tồn tại → ném NotFoundException, không query interview")
        void getAll_jobNotFound_throwsNotFound() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAll(userId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(interviewRepository).should(never()).findAllByJobIdAndUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Happy path: tìm thấy interview → trả về response")
        void getById_found_returnsResponse() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));

            InterviewResponse result = service.getById(userId, jobId, interviewId);

            assertThat(result.id()).isEqualTo(interviewId);
            assertThat(result.jobId()).isEqualTo(jobId);
            assertThat(result.round()).isEqualTo(1);
        }

        @Test
        @DisplayName("Interview không tồn tại → ném NotFoundException (404)")
        void getById_notFound_throwsNotFound() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(userId, jobId, interviewId))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );
        }

        @Test
        @DisplayName("Interview thuộc job khác → ném NotFoundException")
        void getById_wrongJob_throwsNotFound() {
            UUID otherJobId = UUID.randomUUID();
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, otherJobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(userId, otherJobId, interviewId))
                    .isInstanceOf(AppException.NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private UpdateInterviewRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateInterviewRequest(
                    null, 2, InterviewType.ONSITE,
                    scheduledAt.plusDays(1), 90, "HCM Office",
                    "Ôn DSA", "Hỏi về HashMap", "Trả lời được", "Good"
            );
        }

        @Test
        @DisplayName("Happy path: cập nhật thành công → các field được set đúng")
        void update_success_setsAllFields() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));
            given(interviewRepository.save(any(Interview.class))).willReturn(mockInterview);

            service.update(userId, jobId, interviewId, request);

            assertThat(mockInterview.getRound()).isEqualTo(2);
            assertThat(mockInterview.getInterviewType()).isEqualTo(InterviewType.ONSITE);
            assertThat(mockInterview.getScheduledAt()).isEqualTo(scheduledAt.plusDays(1));
            assertThat(mockInterview.getDurationMinutes()).isEqualTo(90);
            assertThat(mockInterview.getLocation()).isEqualTo("HCM Office");
            assertThat(mockInterview.getPreparationNote()).isEqualTo("Ôn DSA");
            assertThat(mockInterview.getQuestionsAsked()).isEqualTo("Hỏi về HashMap");
            assertThat(mockInterview.getMyAnswers()).isEqualTo("Trả lời được");
            assertThat(mockInterview.getFeedback()).isEqualTo("Good");
        }

        @Test
        @DisplayName("round null → giữ nguyên round cũ")
        void update_nullRound_keepsExistingRound() {
            UpdateInterviewRequest noRoundRequest = new UpdateInterviewRequest(
                    null, null, null, scheduledAt, null, null, null, null, null, null
            );
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));
            given(interviewRepository.save(any())).willReturn(mockInterview);

            service.update(userId, jobId, interviewId, noRoundRequest);

            assertThat(mockInterview.getRound()).isEqualTo(1);
        }

        @Test
        @DisplayName("interviewType null → giữ nguyên type cũ")
        void update_nullInterviewType_keepsExistingType() {
            UpdateInterviewRequest noTypeRequest = new UpdateInterviewRequest(
                    null, null, null, scheduledAt, null, null, null, null, null, null
            );
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));
            given(interviewRepository.save(any())).willReturn(mockInterview);

            service.update(userId, jobId, interviewId, noTypeRequest);

            assertThat(mockInterview.getInterviewType()).isEqualTo(InterviewType.TECHNICAL);
        }

        @Test
        @DisplayName("contactId null → set null vào interview (xóa contact)")
        void update_nullContactId_setsNull() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));
            given(interviewRepository.save(any())).willReturn(mockInterview);

            service.update(userId, jobId, interviewId, request);

            assertThat(mockInterview.getContactId()).isNull();
        }

        @Test
        @DisplayName("Interview không tồn tại → ném NotFoundException, không save")
        void update_notFound_throwsNotFound() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(userId, jobId, interviewId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(interviewRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateResult()")
    class UpdateResultTests {

        @Test
        @DisplayName("Happy path: cập nhật result thành công")
        void updateResult_success_setsResult() {
            UpdateInterviewResultRequest request = new UpdateInterviewResultRequest(
                    InterviewResult.PASSED, "Hỏi về Spring", "Trả lời ổn", "Positive"
            );
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));
            given(interviewRepository.save(any(Interview.class))).willReturn(mockInterview);

            InterviewResponse result = service.updateResult(userId, jobId, interviewId, request);

            assertThat(mockInterview.getResult()).isEqualTo(InterviewResult.PASSED);
            assertThat(mockInterview.getQuestionsAsked()).isEqualTo("Hỏi về Spring");
            assertThat(mockInterview.getMyAnswers()).isEqualTo("Trả lời ổn");
            assertThat(mockInterview.getFeedback()).isEqualTo("Positive");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("questionsAsked null → giữ nguyên giá trị cũ")
        void updateResult_nullQuestionsAsked_keepsExisting() {
            mockInterview.setQuestionsAsked("Câu hỏi cũ");
            UpdateInterviewResultRequest request = new UpdateInterviewResultRequest(
                    InterviewResult.FAILED, null, null, null
            );
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));
            given(interviewRepository.save(any())).willReturn(mockInterview);

            service.updateResult(userId, jobId, interviewId, request);

            assertThat(mockInterview.getQuestionsAsked()).isEqualTo("Câu hỏi cũ");
        }

        @Test
        @DisplayName("myAnswers null → giữ nguyên giá trị cũ")
        void updateResult_nullMyAnswers_keepsExisting() {
            mockInterview.setMyAnswers("Câu trả lời cũ");
            UpdateInterviewResultRequest request = new UpdateInterviewResultRequest(
                    InterviewResult.PASSED, null, null, null
            );
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));
            given(interviewRepository.save(any())).willReturn(mockInterview);

            service.updateResult(userId, jobId, interviewId, request);

            assertThat(mockInterview.getMyAnswers()).isEqualTo("Câu trả lời cũ");
        }

        @Test
        @DisplayName("feedback null → giữ nguyên feedback cũ")
        void updateResult_nullFeedback_keepsExisting() {
            mockInterview.setFeedback("Feedback cũ");
            UpdateInterviewResultRequest request = new UpdateInterviewResultRequest(
                    InterviewResult.PASSED, null, null, null
            );
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));
            given(interviewRepository.save(any())).willReturn(mockInterview);

            service.updateResult(userId, jobId, interviewId, request);

            assertThat(mockInterview.getFeedback()).isEqualTo("Feedback cũ");
        }

        @Test
        @DisplayName("Interview không tồn tại → ném NotFoundException")
        void updateResult_notFound_throwsNotFound() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateResult(userId, jobId, interviewId,
                    new UpdateInterviewResultRequest(InterviewResult.PASSED, null, null, null)))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(interviewRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Happy path: xóa thành công → soft delete")
        void delete_success_softDeletesInterview() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));

            service.delete(userId, jobId, interviewId);

            assertThat(mockInterview.isDeleted()).isTrue();
            assertThat(mockInterview.getDeletedAt()).isNotNull();
            then(interviewRepository).should().save(mockInterview);
        }

        @Test
        @DisplayName("Interview không tồn tại → ném NotFoundException, không soft delete")
        void delete_notFound_throwsNotFound() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(userId, jobId, interviewId))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(interviewRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Interview thuộc user khác → ném NotFoundException, không soft delete")
        void delete_wrongUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(otherUserId, jobId, interviewId))
                    .isInstanceOf(AppException.NotFoundException.class);

            assertThat(mockInterview.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("toResponse() — field mapping")
    class ToResponseTests {

        @Test
        @DisplayName("Tất cả field được map đúng sang InterviewResponse")
        void toResponse_mapsAllFieldsCorrectly() {
            UUID contactId = UUID.randomUUID();
            mockInterview.setContactId(contactId);
            mockInterview.setQuestionsAsked("HashMap là gì?");
            mockInterview.setMyAnswers("Là hash table");
            mockInterview.setFeedback("Good answer");

            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));

            InterviewResponse result = service.getById(userId, jobId, interviewId);

            assertThat(result.id()).isEqualTo(interviewId);
            assertThat(result.jobId()).isEqualTo(jobId);
            assertThat(result.contactId()).isEqualTo(contactId);
            assertThat(result.round()).isEqualTo(1);
            assertThat(result.interviewType()).isEqualTo(InterviewType.TECHNICAL);
            assertThat(result.scheduledAt()).isEqualTo(scheduledAt);
            assertThat(result.durationMinutes()).isEqualTo(60);
            assertThat(result.location()).isEqualTo("Google Meet");
            assertThat(result.result()).isEqualTo(InterviewResult.PENDING);
            assertThat(result.preparationNote()).isEqualTo("Review Java core");
            assertThat(result.questionsAsked()).isEqualTo("HashMap là gì?");
            assertThat(result.myAnswers()).isEqualTo("Là hash table");
            assertThat(result.feedback()).isEqualTo("Good answer");
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("contactId null → map null vào response")
        void toResponse_nullContactId_returnsNullContactId() {
            given(interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId))
                    .willReturn(Optional.of(mockInterview));

            InterviewResponse result = service.getById(userId, jobId, interviewId);

            assertThat(result.contactId()).isNull();
        }
    }
}