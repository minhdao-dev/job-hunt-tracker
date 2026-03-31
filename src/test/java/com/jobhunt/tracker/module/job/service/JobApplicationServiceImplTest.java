package com.jobhunt.tracker.module.job.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.auth.repository.UserRepository;
import com.jobhunt.tracker.module.company.entity.Company;
import com.jobhunt.tracker.module.company.entity.CompanySize;
import com.jobhunt.tracker.module.company.repository.CompanyRepository;
import com.jobhunt.tracker.module.job.dto.*;
import com.jobhunt.tracker.module.job.entity.*;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.job.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
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
@DisplayName("JobApplicationServiceImpl Tests")
class JobApplicationServiceImplTest {

    @Mock
    private JobApplicationRepository jobRepository;
    @Mock
    private StatusHistoryRepository historyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private JobApplicationServiceImpl service;

    private User mockUser;
    private Company mockCompany;
    private JobApplication mockJob;

    private final UUID userId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        String email = "dao@example.com";
        mockUser = User.builder()
                .email(email)
                .passwordHash("$2a$10$hashed")
                .fullName("Nguyen Minh Dao")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", userId);

        mockCompany = Company.builder()
                .user(mockUser)
                .name("Google")
                .size(CompanySize.ENTERPRISE)
                .isOutsource(false)
                .build();
        ReflectionTestUtils.setField(mockCompany, "id", companyId);

        mockJob = JobApplication.builder()
                .user(mockUser)
                .company(mockCompany)
                .position("Java Backend Developer")
                .jobUrl("https://jobs.google.com/1")
                .appliedDate(LocalDate.now())
                .source(JobSource.LINKEDIN)
                .status(JobStatus.APPLIED)
                .priority(JobPriority.HIGH)
                .salaryMin(20_000_000L)
                .salaryMax(30_000_000L)
                .currency("VND")
                .isRemote(false)
                .notes("Dream job")
                .build();
        ReflectionTestUtils.setField(mockJob, "id", jobId);
        ReflectionTestUtils.setField(mockJob, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(mockJob, "updatedAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        private CreateJobApplicationRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateJobApplicationRequest(
                    companyId, "Java Backend Developer",
                    "https://jobs.google.com/1", "Job description",
                    LocalDate.now(), JobSource.LINKEDIN, JobPriority.HIGH,
                    20_000_000L, 30_000_000L, "VND", false, "Dream job"
            );
        }

        @Test
        @DisplayName("Happy path: tạo job application thành công → trả về response")
        void create_success_returnsResponse() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);
            given(historyRepository.save(any(StatusHistory.class)))
                    .willReturn(StatusHistory.builder().build());

            JobApplicationResponse result = service.create(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.position()).isEqualTo("Java Backend Developer");
            assertThat(result.companyName()).isEqualTo("Google");
            assertThat(result.status()).isEqualTo(JobStatus.APPLIED);
            assertThat(result.salaryMin()).isEqualTo(20_000_000L);
            assertThat(result.salaryMax()).isEqualTo(30_000_000L);
        }

        @Test
        @DisplayName("Tạo thành công → status luôn là APPLIED dù request không gửi")
        void create_success_statusAlwaysApplied() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);
            given(historyRepository.save(any())).willReturn(StatusHistory.builder().build());

            service.create(userId, request);

            ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
            then(jobRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.APPLIED);
        }

        @Test
        @DisplayName("Tạo thành công → StatusHistory được lưu với oldStatus null")
        void create_success_recordsInitialStatusHistory() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);
            given(historyRepository.save(any(StatusHistory.class)))
                    .willReturn(StatusHistory.builder().build());

            service.create(userId, request);

            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            then(historyRepository).should().save(captor.capture());

            StatusHistory history = captor.getValue();
            assertThat(history.getOldStatus()).isNull();
            assertThat(history.getNewStatus()).isEqualTo(JobStatus.APPLIED);
        }

        @Test
        @DisplayName("companyId null → tạo job không gắn company")
        void create_nullCompanyId_createsJobWithoutCompany() {
            CreateJobApplicationRequest noCompanyRequest = new CreateJobApplicationRequest(
                    null, "Backend Dev", null, null,
                    null, null, null, null, null, null, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            JobApplication jobNoCompany = JobApplication.builder()
                    .user(mockUser)
                    .position("Backend Dev")
                    .status(JobStatus.APPLIED)
                    .appliedDate(LocalDate.now())
                    .build();
            ReflectionTestUtils.setField(jobNoCompany, "id", jobId);
            ReflectionTestUtils.setField(jobNoCompany, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(jobNoCompany, "updatedAt", LocalDateTime.now());

            given(jobRepository.save(any(JobApplication.class))).willReturn(jobNoCompany);
            given(historyRepository.save(any())).willReturn(StatusHistory.builder().build());

            JobApplicationResponse result = service.create(userId, noCompanyRequest);

            assertThat(result.companyId()).isNull();
            assertThat(result.companyName()).isNull();
            then(companyRepository).should(never()).findByIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("appliedDate null → default là ngày hôm nay")
        void create_nullAppliedDate_defaultsToToday() {
            CreateJobApplicationRequest noDateRequest = new CreateJobApplicationRequest(
                    null, "Backend Dev", null, null,
                    null, null, null, null, null, null, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);
            given(historyRepository.save(any())).willReturn(StatusHistory.builder().build());

            service.create(userId, noDateRequest);

            ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
            then(jobRepository).should().save(captor.capture());
            assertThat(captor.getValue().getAppliedDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("source null → default OTHER")
        void create_nullSource_defaultsToOther() {
            CreateJobApplicationRequest noSourceRequest = new CreateJobApplicationRequest(
                    null, "Backend Dev", null, null,
                    null, null, null, null, null, null, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);
            given(historyRepository.save(any())).willReturn(StatusHistory.builder().build());

            service.create(userId, noSourceRequest);

            ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
            then(jobRepository).should().save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo(JobSource.OTHER);
        }

        @Test
        @DisplayName("priority null → default MEDIUM")
        void create_nullPriority_defaultsToMedium() {
            CreateJobApplicationRequest noPriorityRequest = new CreateJobApplicationRequest(
                    null, "Backend Dev", null, null,
                    null, null, null, null, null, null, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);
            given(historyRepository.save(any())).willReturn(StatusHistory.builder().build());

            service.create(userId, noPriorityRequest);

            ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
            then(jobRepository).should().save(captor.capture());
            assertThat(captor.getValue().getPriority()).isEqualTo(JobPriority.MEDIUM);
        }

        @Test
        @DisplayName("salaryMin > salaryMax → ném BadRequestException (400)")
        void create_salaryMinExceedsMax_throwsBadRequest() {
            CreateJobApplicationRequest badSalaryRequest = new CreateJobApplicationRequest(
                    null, "Backend Dev", null, null,
                    null, null, null, 30_000_000L, 20_000_000L, null, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> service.create(userId, badSalaryRequest))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex ->
                            assertThat(ex.getMessage())
                                    .contains("Salary min must not exceed salary max")
                    );

            then(jobRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Company không thuộc user → ném NotFoundException")
        void create_companyNotOwnedByUser_throwsNotFound() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(userId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(jobRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("User không tồn tại → ném NotFoundException")
        void create_userNotFound_throwsNotFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(userId, request))
                    .isInstanceOf(AppException.NotFoundException.class);
        }

        @Test
        @DisplayName("User đã bị soft delete → ném NotFoundException")
        void create_deletedUser_throwsNotFound() {
            mockUser.softDelete();
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> service.create(userId, request))
                    .isInstanceOf(AppException.NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        private Pageable pageable;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10, Sort.by("appliedDate").descending());
        }

        @Test
        @DisplayName("Không filter → trả về tất cả job của user")
        void getAll_noFilter_returnsAll() {
            Page<JobApplication> page = new PageImpl<>(List.of(mockJob), pageable, 1);
            given(jobRepository.findAllByUserId(userId, pageable)).willReturn(page);

            Page<JobApplicationResponse> result = service.getAll(userId, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).position()).isEqualTo("Java Backend Developer");
        }

        @Test
        @DisplayName("Filter theo status → gọi findAllByUserIdAndStatus")
        void getAll_withStatus_filtersCorrectly() {
            Page<JobApplication> page = new PageImpl<>(List.of(mockJob), pageable, 1);
            given(jobRepository.findAllByUserIdAndStatus(userId, JobStatus.APPLIED, pageable))
                    .willReturn(page);

            Page<JobApplicationResponse> result = service.getAll(
                    userId, JobStatus.APPLIED, null, pageable
            );

            assertThat(result.getContent()).hasSize(1);
            then(jobRepository).should(never()).findAllByUserId(any(), any());
            then(jobRepository).should(never()).searchByKeyword(any(), any(), any());
        }

        @Test
        @DisplayName("Có keyword → search theo position hoặc company name")
        void getAll_withKeyword_searchesCorrectly() {
            Page<JobApplication> page = new PageImpl<>(List.of(mockJob), pageable, 1);
            given(jobRepository.searchByKeyword(userId, "java", pageable)).willReturn(page);

            Page<JobApplicationResponse> result = service.getAll(
                    userId, null, "java", pageable
            );

            assertThat(result.getContent()).hasSize(1);
            then(jobRepository).should(never()).findAllByUserId(any(), any());
            then(jobRepository).should(never()).findAllByUserIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("Keyword có khoảng trắng → trim trước khi search")
        void getAll_keywordWithSpaces_trimsBeforeSearch() {
            Page<JobApplication> page = new PageImpl<>(List.of(), pageable, 0);
            given(jobRepository.searchByKeyword(eq(userId), eq("java"), eq(pageable)))
                    .willReturn(page);

            service.getAll(userId, null, "  java  ", pageable);

            then(jobRepository).should().searchByKeyword(userId, "java", pageable);
        }

        @Test
        @DisplayName("Keyword blank → trả về tất cả (không search)")
        void getAll_blankKeyword_returnsAll() {
            Page<JobApplication> page = new PageImpl<>(List.of(mockJob), pageable, 1);
            given(jobRepository.findAllByUserId(userId, pageable)).willReturn(page);

            service.getAll(userId, null, "   ", pageable);

            then(jobRepository).should().findAllByUserId(userId, pageable);
            then(jobRepository).should(never()).searchByKeyword(any(), any(), any());
        }

        @Test
        @DisplayName("Keyword ưu tiên hơn status khi cả hai đều có")
        void getAll_keywordTakesPriorityOverStatus() {
            Page<JobApplication> page = new PageImpl<>(List.of(mockJob), pageable, 1);
            given(jobRepository.searchByKeyword(userId, "java", pageable)).willReturn(page);

            service.getAll(userId, JobStatus.APPLIED, "java", pageable);

            then(jobRepository).should().searchByKeyword(userId, "java", pageable);
            then(jobRepository).should(never()).findAllByUserIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("Không có job → trả về page rỗng")
        void getAll_noJobs_returnsEmptyPage() {
            Page<JobApplication> empty = new PageImpl<>(List.of(), pageable, 0);
            given(jobRepository.findAllByUserId(userId, pageable)).willReturn(empty);

            Page<JobApplicationResponse> result = service.getAll(userId, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Happy path: tìm thấy job → trả về response")
        void getById_found_returnsResponse() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));

            JobApplicationResponse result = service.getById(userId, jobId);

            assertThat(result.id()).isEqualTo(jobId);
            assertThat(result.position()).isEqualTo("Java Backend Developer");
        }

        @Test
        @DisplayName("Job không tồn tại → ném NotFoundException (404)")
        void getById_notFound_throwsNotFound() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(userId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );
        }

        @Test
        @DisplayName("Job thuộc user khác → ném NotFoundException")
        void getById_wrongUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(jobRepository.findByIdAndUserId(jobId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(otherUserId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private UpdateJobApplicationRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateJobApplicationRequest(
                    companyId, "Senior Java Backend Developer",
                    "https://jobs.google.com/2", "Updated description",
                    LocalDate.now().plusDays(1), JobSource.TOPCV, JobPriority.MEDIUM,
                    25_000_000L, 35_000_000L, "VND", true, "Updated notes"
            );
        }

        @Test
        @DisplayName("Happy path: cập nhật thành công → trả về response mới")
        void update_success_returnsUpdatedResponse() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);

            JobApplicationResponse result = service.update(userId, jobId, request);

            assertThat(result).isNotNull();
            then(jobRepository).should().save(mockJob);
        }

        @Test
        @DisplayName("Cập nhật thành công → các field được set đúng")
        void update_success_setsAllFields() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);

            service.update(userId, jobId, request);

            assertThat(mockJob.getPosition()).isEqualTo("Senior Java Backend Developer");
            assertThat(mockJob.getSource()).isEqualTo(JobSource.TOPCV);
            assertThat(mockJob.getPriority()).isEqualTo(JobPriority.MEDIUM);
            assertThat(mockJob.getSalaryMin()).isEqualTo(25_000_000L);
            assertThat(mockJob.getSalaryMax()).isEqualTo(35_000_000L);
            assertThat(mockJob.getIsRemote()).isTrue();
            assertThat(mockJob.getNotes()).isEqualTo("Updated notes");
        }

        @Test
        @DisplayName("salaryMin > salaryMax → ném BadRequestException")
        void update_salaryMinExceedsMax_throwsBadRequest() {
            UpdateJobApplicationRequest badSalaryRequest = new UpdateJobApplicationRequest(
                    null, "Backend Dev", null, null,
                    null, null, null, 30_000_000L, 20_000_000L, null, null, null
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));

            assertThatThrownBy(() -> service.update(userId, jobId, badSalaryRequest))
                    .isInstanceOf(AppException.BadRequestException.class);

            then(jobRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("companyId null → set company về null")
        void update_nullCompanyId_setsCompanyNull() {
            UpdateJobApplicationRequest noCompanyRequest = new UpdateJobApplicationRequest(
                    null, "Backend Dev", null, null,
                    null, null, null, null, null, null, null, null
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(jobRepository.save(any())).willReturn(mockJob);

            service.update(userId, jobId, noCompanyRequest);

            assertThat(mockJob.getCompany()).isNull();
            then(companyRepository).should(never()).findByIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Job không tồn tại → ném NotFoundException")
        void update_jobNotFound_throwsNotFound() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(userId, jobId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(jobRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("Happy path: đổi status thành công → lưu history + trả về response")
        void updateStatus_success_recordsHistoryAndReturnsResponse() {
            UpdateStatusRequest request = new UpdateStatusRequest(
                    JobStatus.INTERVIEWING, "Got a call"
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(jobRepository.save(any(JobApplication.class))).willReturn(mockJob);
            given(historyRepository.save(any(StatusHistory.class)))
                    .willReturn(StatusHistory.builder().build());

            JobApplicationResponse result = service.updateStatus(userId, jobId, request);

            assertThat(result).isNotNull();
            assertThat(mockJob.getStatus()).isEqualTo(JobStatus.INTERVIEWING);
        }

        @Test
        @DisplayName("StatusHistory được lưu với oldStatus và newStatus đúng")
        void updateStatus_success_savesHistoryWithCorrectStatuses() {
            mockJob.setStatus(JobStatus.APPLIED);
            UpdateStatusRequest request = new UpdateStatusRequest(
                    JobStatus.INTERVIEWING, "Got a call"
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(jobRepository.save(any())).willReturn(mockJob);
            given(historyRepository.save(any(StatusHistory.class)))
                    .willReturn(StatusHistory.builder().build());

            service.updateStatus(userId, jobId, request);

            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            then(historyRepository).should().save(captor.capture());

            StatusHistory history = captor.getValue();
            assertThat(history.getOldStatus()).isEqualTo(JobStatus.APPLIED);
            assertThat(history.getNewStatus()).isEqualTo(JobStatus.INTERVIEWING);
            assertThat(history.getNote()).isEqualTo("Got a call");
        }

        @Test
        @DisplayName("Status giống hiện tại → ném BadRequestException")
        void updateStatus_sameStatus_throwsBadRequest() {
            mockJob.setStatus(JobStatus.APPLIED);
            UpdateStatusRequest request = new UpdateStatusRequest(JobStatus.APPLIED, null);
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));

            assertThatThrownBy(() -> service.updateStatus(userId, jobId, request))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex ->
                            assertThat(ex.getMessage()).contains("APPLIED")
                    );

            then(jobRepository).should(never()).save(any());
            then(historyRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Job không tồn tại → ném NotFoundException")
        void updateStatus_jobNotFound_throwsNotFound() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatus(userId, jobId,
                    new UpdateStatusRequest(JobStatus.INTERVIEWING, null)))
                    .isInstanceOf(AppException.NotFoundException.class);
        }

        @Test
        @DisplayName("note null → vẫn lưu history bình thường")
        void updateStatus_nullNote_savesHistoryWithoutNote() {
            mockJob.setStatus(JobStatus.APPLIED);
            UpdateStatusRequest request = new UpdateStatusRequest(JobStatus.REJECTED, null);
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(jobRepository.save(any())).willReturn(mockJob);
            given(historyRepository.save(any(StatusHistory.class)))
                    .willReturn(StatusHistory.builder().build());

            assertThatCode(() -> service.updateStatus(userId, jobId, request))
                    .doesNotThrowAnyException();

            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            then(historyRepository).should().save(captor.capture());
            assertThat(captor.getValue().getNote()).isNull();
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Happy path: xóa thành công → soft delete")
        void delete_success_softDeletesJob() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));

            service.delete(userId, jobId);

            assertThat(mockJob.isDeleted()).isTrue();
            assertThat(mockJob.getDeletedAt()).isNotNull();
            then(jobRepository).should().save(mockJob);
        }

        @Test
        @DisplayName("Job không tồn tại → ném NotFoundException")
        void delete_notFound_throwsNotFound() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(userId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(jobRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("getStatusHistory()")
    class GetStatusHistoryTests {

        @Test
        @DisplayName("Happy path: trả về danh sách history theo thứ tự thời gian")
        void getStatusHistory_success_returnsOrderedHistory() {
            StatusHistory h1 = StatusHistory.builder()
                    .job(mockJob).oldStatus(null)
                    .newStatus(JobStatus.APPLIED)
                    .note("Initial").changedAt(LocalDateTime.now().minusDays(3))
                    .build();
            ReflectionTestUtils.setField(h1, "id", UUID.randomUUID());

            StatusHistory h2 = StatusHistory.builder()
                    .job(mockJob).oldStatus(JobStatus.APPLIED)
                    .newStatus(JobStatus.INTERVIEWING)
                    .note("Got call").changedAt(LocalDateTime.now().minusDays(1))
                    .build();
            ReflectionTestUtils.setField(h2, "id", UUID.randomUUID());

            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(historyRepository.findAllByJobId(jobId)).willReturn(List.of(h1, h2));

            List<StatusHistoryResponse> result = service.getStatusHistory(userId, jobId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).oldStatus()).isNull();
            assertThat(result.get(0).newStatus()).isEqualTo(JobStatus.APPLIED);
            assertThat(result.get(1).oldStatus()).isEqualTo(JobStatus.APPLIED);
            assertThat(result.get(1).newStatus()).isEqualTo(JobStatus.INTERVIEWING);
        }

        @Test
        @DisplayName("Job không tồn tại → ném NotFoundException trước khi query history")
        void getStatusHistory_jobNotFound_throwsNotFound() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStatusHistory(userId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(historyRepository).should(never()).findAllByJobId(any());
        }

        @Test
        @DisplayName("Chưa có history → trả về list rỗng")
        void getStatusHistory_noHistory_returnsEmptyList() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(historyRepository.findAllByJobId(jobId)).willReturn(List.of());

            List<StatusHistoryResponse> result = service.getStatusHistory(userId, jobId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("toResponse() — field mapping")
    class ToResponseTests {

        @Test
        @DisplayName("Tất cả field được map đúng sang JobApplicationResponse")
        void toResponse_mapsAllFieldsCorrectly() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));

            JobApplicationResponse result = service.getById(userId, jobId);

            assertThat(result.id()).isEqualTo(jobId);
            assertThat(result.companyId()).isEqualTo(companyId);
            assertThat(result.companyName()).isEqualTo("Google");
            assertThat(result.position()).isEqualTo("Java Backend Developer");
            assertThat(result.source()).isEqualTo(JobSource.LINKEDIN);
            assertThat(result.status()).isEqualTo(JobStatus.APPLIED);
            assertThat(result.priority()).isEqualTo(JobPriority.HIGH);
            assertThat(result.salaryMin()).isEqualTo(20_000_000L);
            assertThat(result.salaryMax()).isEqualTo(30_000_000L);
            assertThat(result.currency()).isEqualTo("VND");
            assertThat(result.isRemote()).isFalse();
            assertThat(result.notes()).isEqualTo("Dream job");
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Job không có company → companyId và companyName là null")
        void toResponse_noCompany_returnsNullCompanyFields() {
            JobApplication jobNoCompany = JobApplication.builder()
                    .user(mockUser)
                    .position("Backend Dev")
                    .status(JobStatus.APPLIED)
                    .appliedDate(LocalDate.now())
                    .build();
            ReflectionTestUtils.setField(jobNoCompany, "id", jobId);
            ReflectionTestUtils.setField(jobNoCompany, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(jobNoCompany, "updatedAt", LocalDateTime.now());

            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(jobNoCompany));

            JobApplicationResponse result = service.getById(userId, jobId);

            assertThat(result.companyId()).isNull();
            assertThat(result.companyName()).isNull();
        }
    }
}