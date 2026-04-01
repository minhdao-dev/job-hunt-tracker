package com.jobhunt.tracker.module.reminder.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import com.jobhunt.tracker.module.job.entity.JobStatus;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.reminder.dto.*;
import com.jobhunt.tracker.module.reminder.entity.Reminder;
import com.jobhunt.tracker.module.reminder.repository.ReminderRepository;
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
@DisplayName("ReminderServiceImpl Tests")
class ReminderServiceImplTest {

    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private JobApplicationRepository jobRepository;

    @InjectMocks
    private ReminderServiceImpl service;

    private JobApplication mockJob;
    private Reminder mockReminder;

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID reminderId = UUID.randomUUID();
    private final LocalDateTime remindAt = LocalDateTime.now().plusDays(3);

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
                .status(JobStatus.APPLIED)
                .build();
        ReflectionTestUtils.setField(mockJob, "id", jobId);

        mockReminder = Reminder.builder()
                .job(mockJob)
                .remindAt(remindAt)
                .message("Follow up với HR")
                .isSent(false)
                .build();
        ReflectionTestUtils.setField(mockReminder, "id", reminderId);
        ReflectionTestUtils.setField(mockReminder, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(mockReminder, "updatedAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        private CreateReminderRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateReminderRequest(remindAt, "Follow up với HR");
        }

        @Test
        @DisplayName("Happy path: tạo reminder thành công → trả về response")
        void create_success_returnsResponse() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(reminderRepository.save(any(Reminder.class))).willReturn(mockReminder);

            ReminderResponse result = service.create(userId, jobId, request);

            assertThat(result).isNotNull();
            assertThat(result.jobId()).isEqualTo(jobId);
            assertThat(result.jobPosition()).isEqualTo("Java Backend Developer");
            assertThat(result.remindAt()).isEqualTo(remindAt);
            assertThat(result.message()).isEqualTo("Follow up với HR");
            assertThat(result.isSent()).isFalse();
        }

        @Test
        @DisplayName("Tạo thành công → Reminder được save với đúng dữ liệu")
        void create_success_savesWithCorrectData() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(reminderRepository.save(any(Reminder.class))).willReturn(mockReminder);

            service.create(userId, jobId, request);

            ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
            then(reminderRepository).should().save(captor.capture());

            Reminder saved = captor.getValue();
            assertThat(saved.getJob()).isEqualTo(mockJob);
            assertThat(saved.getRemindAt()).isEqualTo(remindAt);
            assertThat(saved.getMessage()).isEqualTo("Follow up với HR");
            assertThat(saved.getIsSent()).isFalse();
        }

        @Test
        @DisplayName("message null → tạo vẫn thành công")
        void create_nullMessage_savesWithoutMessage() {
            CreateReminderRequest noMessageRequest = new CreateReminderRequest(remindAt, null);
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(reminderRepository.save(any(Reminder.class))).willReturn(mockReminder);

            service.create(userId, jobId, noMessageRequest);

            ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
            then(reminderRepository).should().save(captor.capture());
            assertThat(captor.getValue().getMessage()).isNull();
        }

        @Test
        @DisplayName("isSent mặc định là false khi tạo mới")
        void create_success_isSentDefaultsFalse() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(reminderRepository.save(any(Reminder.class))).willReturn(mockReminder);

            service.create(userId, jobId, request);

            ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
            then(reminderRepository).should().save(captor.capture());
            assertThat(captor.getValue().getIsSent()).isFalse();
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

            then(reminderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Job thuộc user khác → ném NotFoundException")
        void create_jobBelongsToOtherUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(jobRepository.findByIdAndUserId(jobId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(otherUserId, jobId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(reminderRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Happy path: trả về danh sách reminder của job theo remindAt ASC")
        void getAll_success_returnsOrderedList() {
            Reminder r2 = Reminder.builder()
                    .job(mockJob)
                    .remindAt(remindAt.plusDays(2))
                    .message("Second reminder")
                    .isSent(false)
                    .build();
            ReflectionTestUtils.setField(r2, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(r2, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(r2, "updatedAt", LocalDateTime.now());

            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(reminderRepository.findAllByJobIdAndUserId(jobId, userId))
                    .willReturn(List.of(mockReminder, r2));

            List<ReminderResponse> result = service.getAll(userId, jobId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).message()).isEqualTo("Follow up với HR");
            assertThat(result.get(1).message()).isEqualTo("Second reminder");
        }

        @Test
        @DisplayName("Chưa có reminder nào → trả về list rỗng")
        void getAll_noReminders_returnsEmptyList() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(reminderRepository.findAllByJobIdAndUserId(jobId, userId))
                    .willReturn(List.of());

            List<ReminderResponse> result = service.getAll(userId, jobId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Job không tồn tại → ném NotFoundException, không query reminder")
        void getAll_jobNotFound_throwsNotFound() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAll(userId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(reminderRepository).should(never()).findAllByJobIdAndUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private UpdateReminderRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateReminderRequest(
                    remindAt.plusDays(1), "Updated message"
            );
        }

        @Test
        @DisplayName("Happy path: cập nhật thành công → các field được set đúng")
        void update_success_setsAllFields() {
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.of(mockReminder));
            given(reminderRepository.save(any(Reminder.class))).willReturn(mockReminder);

            service.update(userId, jobId, reminderId, request);

            assertThat(mockReminder.getRemindAt()).isEqualTo(remindAt.plusDays(1));
            assertThat(mockReminder.getMessage()).isEqualTo("Updated message");
            then(reminderRepository).should().save(mockReminder);
        }

        @Test
        @DisplayName("Update reminder → isSent reset về false (đề phòng đổi thời gian)")
        void update_success_resetIsSentToFalse() {
            mockReminder.setIsSent(true);
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.of(mockReminder));
            given(reminderRepository.save(any(Reminder.class))).willReturn(mockReminder);

            service.update(userId, jobId, reminderId, request);

            assertThat(mockReminder.getIsSent()).isFalse();
        }

        @Test
        @DisplayName("message null → set null vào reminder")
        void update_nullMessage_setsNull() {
            UpdateReminderRequest noMessageRequest = new UpdateReminderRequest(remindAt, null);
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.of(mockReminder));
            given(reminderRepository.save(any(Reminder.class))).willReturn(mockReminder);

            service.update(userId, jobId, reminderId, noMessageRequest);

            assertThat(mockReminder.getMessage()).isNull();
        }

        @Test
        @DisplayName("Cập nhật thành công → trả về response")
        void update_success_returnsResponse() {
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.of(mockReminder));
            given(reminderRepository.save(any(Reminder.class))).willReturn(mockReminder);

            ReminderResponse result = service.update(userId, jobId, reminderId, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Reminder không tồn tại → ném NotFoundException, không save")
        void update_notFound_throwsNotFound() {
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(userId, jobId, reminderId, request))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );

            then(reminderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Reminder thuộc job khác → ném NotFoundException")
        void update_wrongJob_throwsNotFound() {
            UUID otherJobId = UUID.randomUUID();
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, otherJobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(userId, otherJobId, reminderId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(reminderRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Happy path: xóa thành công → soft delete")
        void delete_success_softDeletesReminder() {
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.of(mockReminder));

            service.delete(userId, jobId, reminderId);

            assertThat(mockReminder.isDeleted()).isTrue();
            assertThat(mockReminder.getDeletedAt()).isNotNull();
            then(reminderRepository).should().save(mockReminder);
        }

        @Test
        @DisplayName("Reminder không tồn tại → ném NotFoundException, không soft delete")
        void delete_notFound_throwsNotFound() {
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(userId, jobId, reminderId))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(reminderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Reminder thuộc user khác → ném NotFoundException, không soft delete")
        void delete_wrongUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(otherUserId, jobId, reminderId))
                    .isInstanceOf(AppException.NotFoundException.class);

            assertThat(mockReminder.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("toResponse() — field mapping")
    class ToResponseTests {

        @Test
        @DisplayName("Tất cả field được map đúng sang ReminderResponse")
        void toResponse_mapsAllFieldsCorrectly() {
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.of(mockReminder));
            given(reminderRepository.save(any())).willReturn(mockReminder);

            UpdateReminderRequest req = new UpdateReminderRequest(remindAt, "Follow up với HR");
            ReminderResponse result = service.update(userId, jobId, reminderId, req);

            assertThat(result.id()).isEqualTo(reminderId);
            assertThat(result.jobId()).isEqualTo(jobId);
            assertThat(result.jobPosition()).isEqualTo("Java Backend Developer");
            assertThat(result.message()).isEqualTo("Follow up với HR");
            assertThat(result.remindAt()).isEqualTo(remindAt);
            assertThat(result.isSent()).isFalse();
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("message null → map null vào response")
        void toResponse_nullMessage_returnsNullMessage() {
            mockReminder.setMessage(null);
            given(reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId))
                    .willReturn(Optional.of(mockReminder));
            given(reminderRepository.save(any())).willReturn(mockReminder);

            UpdateReminderRequest req = new UpdateReminderRequest(remindAt, null);
            ReminderResponse result = service.update(userId, jobId, reminderId, req);

            assertThat(result.message()).isNull();
        }

        @Test
        @DisplayName("isSent = true → map đúng vào response")
        void toResponse_isSentTrue_returnsTrueInResponse() {
            mockReminder.setIsSent(true);
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(reminderRepository.findAllByJobIdAndUserId(jobId, userId))
                    .willReturn(List.of(mockReminder));

            List<ReminderResponse> result = service.getAll(userId, jobId);

            assertThat(result.get(0).isSent()).isTrue();
        }
    }
}