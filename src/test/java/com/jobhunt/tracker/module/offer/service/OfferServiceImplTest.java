package com.jobhunt.tracker.module.offer.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import com.jobhunt.tracker.module.job.entity.JobStatus;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.offer.dto.*;
import com.jobhunt.tracker.module.offer.entity.Offer;
import com.jobhunt.tracker.module.offer.entity.OfferDecision;
import com.jobhunt.tracker.module.offer.repository.OfferRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfferServiceImpl Tests")
class OfferServiceImplTest {

    @Mock
    private OfferRepository offerRepository;
    @Mock
    private JobApplicationRepository jobRepository;

    @InjectMocks
    private OfferServiceImpl service;

    private JobApplication mockJob;
    private Offer mockOffer;

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID offerId = UUID.randomUUID();

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
                .status(JobStatus.OFFERED)
                .build();
        ReflectionTestUtils.setField(mockJob, "id", jobId);

        mockOffer = Offer.builder()
                .job(mockJob)
                .salary(25_000_000L)
                .currency("VND")
                .benefits("BHYT, BHXH, 13th month")
                .startDate(LocalDate.now().plusMonths(1))
                .expiredAt(LocalDate.now().plusDays(7))
                .decision(OfferDecision.PENDING)
                .note("Looking forward to joining")
                .build();
        ReflectionTestUtils.setField(mockOffer, "id", offerId);
        ReflectionTestUtils.setField(mockOffer, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(mockOffer, "updatedAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        private CreateOfferRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateOfferRequest(
                    25_000_000L, "VND",
                    "BHYT, BHXH, 13th month",
                    LocalDate.now().plusMonths(1),
                    LocalDate.now().plusDays(7),
                    "Looking forward to joining"
            );
        }

        @Test
        @DisplayName("Happy path: tạo offer thành công → trả về response")
        void create_success_returnsResponse() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(offerRepository.existsByJobId(jobId)).willReturn(false);
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            OfferResponse result = service.create(userId, jobId, request);

            assertThat(result).isNotNull();
            assertThat(result.jobId()).isEqualTo(jobId);
            assertThat(result.salary()).isEqualTo(25_000_000L);
            assertThat(result.currency()).isEqualTo("VND");
            assertThat(result.benefits()).isEqualTo("BHYT, BHXH, 13th month");
            assertThat(result.decision()).isEqualTo(OfferDecision.PENDING);
            assertThat(result.note()).isEqualTo("Looking forward to joining");
        }

        @Test
        @DisplayName("Tạo thành công → Offer được save với đúng dữ liệu")
        void create_success_savesWithCorrectData() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(offerRepository.existsByJobId(jobId)).willReturn(false);
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            service.create(userId, jobId, request);

            ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
            then(offerRepository).should().save(captor.capture());

            Offer saved = captor.getValue();
            assertThat(saved.getJob()).isEqualTo(mockJob);
            assertThat(saved.getSalary()).isEqualTo(25_000_000L);
            assertThat(saved.getCurrency()).isEqualTo("VND");
            assertThat(saved.getBenefits()).isEqualTo("BHYT, BHXH, 13th month");
            assertThat(saved.getNote()).isEqualTo("Looking forward to joining");
        }

        @Test
        @DisplayName("currency null → default VND")
        void create_nullCurrency_defaultsToVND() {
            CreateOfferRequest noCurrencyRequest = new CreateOfferRequest(
                    25_000_000L, null, null, null, null, null
            );
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(offerRepository.existsByJobId(jobId)).willReturn(false);
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            service.create(userId, jobId, noCurrencyRequest);

            ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
            then(offerRepository).should().save(captor.capture());
            assertThat(captor.getValue().getCurrency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("decision mặc định là PENDING sau khi tạo")
        void create_success_defaultDecisionIsPending() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(offerRepository.existsByJobId(jobId)).willReturn(false);
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            service.create(userId, jobId, request);

            ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
            then(offerRepository).should().save(captor.capture());
            assertThat(captor.getValue().getDecision()).isEqualTo(OfferDecision.PENDING);
        }

        @Test
        @DisplayName("Job đã có offer → ném ConflictException (409)")
        void create_offerAlreadyExists_throwsConflict() {
            given(jobRepository.findByIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockJob));
            given(offerRepository.existsByJobId(jobId)).willReturn(true);

            assertThatThrownBy(() -> service.create(userId, jobId, request))
                    .isInstanceOf(AppException.ConflictException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(409);
                        assertThat(appEx.getMessage()).contains(jobId.toString());
                    });

            then(offerRepository).should(never()).save(any());
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

            then(offerRepository).should(never()).existsByJobId(any());
            then(offerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Job thuộc user khác → ném NotFoundException")
        void create_jobBelongsToOtherUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(jobRepository.findByIdAndUserId(jobId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(otherUserId, jobId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(offerRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("Happy path: tìm thấy offer → trả về response")
        void get_found_returnsResponse() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));

            OfferResponse result = service.get(userId, jobId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(offerId);
            assertThat(result.jobId()).isEqualTo(jobId);
            assertThat(result.salary()).isEqualTo(25_000_000L);
            assertThat(result.decision()).isEqualTo(OfferDecision.PENDING);
        }

        @Test
        @DisplayName("Offer không tồn tại → ném NotFoundException (404)")
        void get_notFound_throwsNotFound() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(userId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );
        }

        @Test
        @DisplayName("Offer thuộc job của user khác → ném NotFoundException")
        void get_wrongUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(offerRepository.findByJobIdAndUserId(jobId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(otherUserId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private UpdateOfferRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateOfferRequest(
                    30_000_000L, "USD",
                    "Full benefits + stock options",
                    LocalDate.now().plusMonths(2),
                    LocalDate.now().plusDays(14),
                    "Updated note"
            );
        }

        @Test
        @DisplayName("Happy path: cập nhật thành công → các field được set đúng")
        void update_success_setsAllFields() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            service.update(userId, jobId, request);

            assertThat(mockOffer.getSalary()).isEqualTo(30_000_000L);
            assertThat(mockOffer.getCurrency()).isEqualTo("USD");
            assertThat(mockOffer.getBenefits()).isEqualTo("Full benefits + stock options");
            assertThat(mockOffer.getNote()).isEqualTo("Updated note");
            then(offerRepository).should().save(mockOffer);
        }

        @Test
        @DisplayName("currency null → default VND khi update")
        void update_nullCurrency_defaultsToVND() {
            UpdateOfferRequest noCurrencyRequest = new UpdateOfferRequest(
                    30_000_000L, null, null, null, null, null
            );
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            service.update(userId, jobId, noCurrencyRequest);

            assertThat(mockOffer.getCurrency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("salary null → set null (xóa salary)")
        void update_nullSalary_setsNull() {
            UpdateOfferRequest noSalaryRequest = new UpdateOfferRequest(
                    null, "VND", null, null, null, null
            );
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            service.update(userId, jobId, noSalaryRequest);

            assertThat(mockOffer.getSalary()).isNull();
        }

        @Test
        @DisplayName("Cập nhật thành công → trả về response")
        void update_success_returnsResponse() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            OfferResponse result = service.update(userId, jobId, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Offer không tồn tại → ném NotFoundException, không save")
        void update_notFound_throwsNotFound() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(userId, jobId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(offerRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateDecision()")
    class UpdateDecisionTests {

        @Test
        @DisplayName("Happy path: cập nhật decision thành công")
        void updateDecision_success_setsDecision() {
            UpdateOfferDecisionRequest request = new UpdateOfferDecisionRequest(
                    OfferDecision.ACCEPTED, "I accept the offer!"
            );
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            OfferResponse result = service.updateDecision(userId, jobId, request);

            assertThat(mockOffer.getDecision()).isEqualTo(OfferDecision.ACCEPTED);
            assertThat(mockOffer.getNote()).isEqualTo("I accept the offer!");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("note null → giữ nguyên note cũ")
        void updateDecision_nullNote_keepsExistingNote() {
            mockOffer.setNote("Note cũ");
            UpdateOfferDecisionRequest request = new UpdateOfferDecisionRequest(
                    OfferDecision.ACCEPTED, null
            );
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            service.updateDecision(userId, jobId, request);

            assertThat(mockOffer.getNote()).isEqualTo("Note cũ");
        }

        @Test
        @DisplayName("Decision giống hiện tại → ném BadRequestException (400)")
        void updateDecision_sameDecision_throwsBadRequest() {
            mockOffer.setDecision(OfferDecision.PENDING);
            UpdateOfferDecisionRequest request = new UpdateOfferDecisionRequest(
                    OfferDecision.PENDING, null
            );
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));

            assertThatThrownBy(() -> service.updateDecision(userId, jobId, request))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(400);
                        assertThat(appEx.getMessage()).contains("PENDING");
                    });

            then(offerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("NEGOTIATING → ACCEPTED là hợp lệ")
        void updateDecision_negotiatingToAccepted_isValid() {
            mockOffer.setDecision(OfferDecision.NEGOTIATING);
            UpdateOfferDecisionRequest request = new UpdateOfferDecisionRequest(
                    OfferDecision.ACCEPTED, "Final decision"
            );
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));
            given(offerRepository.save(any(Offer.class))).willReturn(mockOffer);

            assertThatCode(() -> service.updateDecision(userId, jobId, request))
                    .doesNotThrowAnyException();

            assertThat(mockOffer.getDecision()).isEqualTo(OfferDecision.ACCEPTED);
        }

        @Test
        @DisplayName("Offer không tồn tại → ném NotFoundException, không save")
        void updateDecision_notFound_throwsNotFound() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateDecision(userId, jobId,
                    new UpdateOfferDecisionRequest(OfferDecision.ACCEPTED, null)))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(offerRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Happy path: xóa thành công → soft delete")
        void delete_success_softDeletesOffer() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));

            service.delete(userId, jobId);

            assertThat(mockOffer.isDeleted()).isTrue();
            assertThat(mockOffer.getDeletedAt()).isNotNull();
            then(offerRepository).should().save(mockOffer);
        }

        @Test
        @DisplayName("Offer không tồn tại → ném NotFoundException, không soft delete")
        void delete_notFound_throwsNotFound() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(userId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(offerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Offer thuộc user khác → ném NotFoundException, không soft delete")
        void delete_wrongUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(offerRepository.findByJobIdAndUserId(jobId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(otherUserId, jobId))
                    .isInstanceOf(AppException.NotFoundException.class);

            assertThat(mockOffer.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("toResponse() — field mapping")
    class ToResponseTests {

        @Test
        @DisplayName("Tất cả field được map đúng sang OfferResponse")
        void toResponse_mapsAllFieldsCorrectly() {
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));

            OfferResponse result = service.get(userId, jobId);

            assertThat(result.id()).isEqualTo(offerId);
            assertThat(result.jobId()).isEqualTo(jobId);
            assertThat(result.salary()).isEqualTo(25_000_000L);
            assertThat(result.currency()).isEqualTo("VND");
            assertThat(result.benefits()).isEqualTo("BHYT, BHXH, 13th month");
            assertThat(result.startDate()).isEqualTo(LocalDate.now().plusMonths(1));
            assertThat(result.expiredAt()).isEqualTo(LocalDate.now().plusDays(7));
            assertThat(result.decision()).isEqualTo(OfferDecision.PENDING);
            assertThat(result.note()).isEqualTo("Looking forward to joining");
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("salary null → map null vào response")
        void toResponse_nullSalary_returnsNullSalary() {
            mockOffer.setSalary(null);
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));

            OfferResponse result = service.get(userId, jobId);

            assertThat(result.salary()).isNull();
        }

        @Test
        @DisplayName("benefits null → map null vào response")
        void toResponse_nullBenefits_returnsNullBenefits() {
            mockOffer.setBenefits(null);
            given(offerRepository.findByJobIdAndUserId(jobId, userId))
                    .willReturn(Optional.of(mockOffer));

            OfferResponse result = service.get(userId, jobId);

            assertThat(result.benefits()).isNull();
        }
    }
}