package com.jobhunt.tracker.module.company.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.auth.repository.UserRepository;
import com.jobhunt.tracker.module.company.dto.*;
import com.jobhunt.tracker.module.company.entity.Company;
import com.jobhunt.tracker.module.company.entity.CompanySize;
import com.jobhunt.tracker.module.company.repository.CompanyRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyServiceImpl Tests")
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyServiceImpl service;

    private User mockUser;
    private Company mockCompany;

    private final UUID userId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

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
                .website("https://google.com")
                .industry("Technology")
                .size(CompanySize.ENTERPRISE)
                .location("Ho Chi Minh City")
                .isOutsource(false)
                .notes("Top company")
                .build();
        ReflectionTestUtils.setField(mockCompany, "id", companyId);
        ReflectionTestUtils.setField(mockCompany, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(mockCompany, "updatedAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        private CreateCompanyRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateCompanyRequest(
                    "Google", "https://google.com",
                    "Technology", CompanySize.ENTERPRISE,
                    "Ho Chi Minh City", false, "Top company"
            );
        }

        @Test
        @DisplayName("Happy path: tạo company thành công → trả về CompanyResponse")
        void create_success_returnsCompanyResponse() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.existsByUserIdAndName(userId, "Google")).willReturn(false);
            given(companyRepository.save(any(Company.class))).willReturn(mockCompany);

            CompanyResponse result = service.create(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Google");
            assertThat(result.website()).isEqualTo("https://google.com");
            assertThat(result.industry()).isEqualTo("Technology");
            assertThat(result.size()).isEqualTo(CompanySize.ENTERPRISE);
            assertThat(result.location()).isEqualTo("Ho Chi Minh City");
            assertThat(result.isOutsource()).isFalse();
            assertThat(result.notes()).isEqualTo("Top company");
        }

        @Test
        @DisplayName("Tạo thành công → Company được save với đúng dữ liệu")
        void create_success_savesCompanyWithCorrectData() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.existsByUserIdAndName(userId, "Google")).willReturn(false);
            given(companyRepository.save(any(Company.class))).willReturn(mockCompany);

            service.create(userId, request);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            then(companyRepository).should().save(captor.capture());

            Company saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Google");
            assertThat(saved.getUser()).isEqualTo(mockUser);
            assertThat(saved.getSize()).isEqualTo(CompanySize.ENTERPRISE);
            assertThat(saved.getIsOutsource()).isFalse();
        }

        @Test
        @DisplayName("size null → default UNKNOWN")
        void create_nullSize_defaultsToUnknown() {
            CreateCompanyRequest nullSizeRequest = new CreateCompanyRequest(
                    "FPT", null, null, null, null, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.existsByUserIdAndName(userId, "FPT")).willReturn(false);
            given(companyRepository.save(any(Company.class))).willReturn(mockCompany);

            service.create(userId, nullSizeRequest);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            then(companyRepository).should().save(captor.capture());
            assertThat(captor.getValue().getSize()).isEqualTo(CompanySize.UNKNOWN);
        }

        @Test
        @DisplayName("isOutsource null → default false")
        void create_nullIsOutsource_defaultsFalse() {
            CreateCompanyRequest nullOutsourceRequest = new CreateCompanyRequest(
                    "FPT", null, null, null, null, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.existsByUserIdAndName(userId, "FPT")).willReturn(false);
            given(companyRepository.save(any(Company.class))).willReturn(mockCompany);

            service.create(userId, nullOutsourceRequest);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            then(companyRepository).should().save(captor.capture());
            assertThat(captor.getValue().getIsOutsource()).isFalse();
        }

        @Test
        @DisplayName("Tên company đã tồn tại → ném ConflictException (409)")
        void create_duplicateName_throwsConflict() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(companyRepository.existsByUserIdAndName(userId, "Google")).willReturn(true);

            assertThatThrownBy(() -> service.create(userId, request))
                    .isInstanceOf(AppException.ConflictException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(409);
                        assertThat(appEx.getMessage()).contains("Google");
                    });

            then(companyRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("User không tồn tại → ném NotFoundException (404)")
        void create_userNotFound_throwsNotFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(userId, request))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );

            then(companyRepository).should(never()).save(any());
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
            pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        }

        @Test
        @DisplayName("Không có keyword → trả về tất cả company của user")
        void getAll_noKeyword_returnsAllCompanies() {
            Page<Company> page = new PageImpl<>(List.of(mockCompany), pageable, 1);
            given(companyRepository.findAllByUserId(userId, pageable)).willReturn(page);

            Page<CompanyResponse> result = service.getAll(userId, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Google");
            then(companyRepository).should(never()).searchByName(any(), any(), any());
        }

        @Test
        @DisplayName("Keyword blank → trả về tất cả (không search)")
        void getAll_blankKeyword_returnsAll() {
            Page<Company> page = new PageImpl<>(List.of(mockCompany), pageable, 1);
            given(companyRepository.findAllByUserId(userId, pageable)).willReturn(page);

            Page<CompanyResponse> result = service.getAll(userId, "   ", pageable);

            assertThat(result.getContent()).hasSize(1);
            then(companyRepository).should(never()).searchByName(any(), any(), any());
        }

        @Test
        @DisplayName("Có keyword → search theo tên")
        void getAll_withKeyword_searchesByName() {
            Page<Company> page = new PageImpl<>(List.of(mockCompany), pageable, 1);
            given(companyRepository.searchByName(userId, "goo", pageable)).willReturn(page);

            Page<CompanyResponse> result = service.getAll(userId, "goo", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Google");
            then(companyRepository).should(never()).findAllByUserId(any(), any());
        }

        @Test
        @DisplayName("Keyword có khoảng trắng → trim trước khi search")
        void getAll_keywordWithSpaces_trimsBeforeSearch() {
            Page<Company> page = new PageImpl<>(List.of(), pageable, 0);
            given(companyRepository.searchByName(eq(userId), eq("goo"), eq(pageable)))
                    .willReturn(page);

            service.getAll(userId, "  goo  ", pageable);

            then(companyRepository).should().searchByName(userId, "goo", pageable);
        }

        @Test
        @DisplayName("Không có company → trả về page rỗng")
        void getAll_noCompanies_returnsEmptyPage() {
            Page<Company> empty = new PageImpl<>(List.of(), pageable, 0);
            given(companyRepository.findAllByUserId(userId, pageable)).willReturn(empty);

            Page<CompanyResponse> result = service.getAll(userId, null, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Happy path: tìm thấy company → trả về CompanyResponse")
        void getById_found_returnsResponse() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));

            CompanyResponse result = service.getById(userId, companyId);

            assertThat(result.id()).isEqualTo(companyId);
            assertThat(result.name()).isEqualTo("Google");
        }

        @Test
        @DisplayName("Company không tồn tại → ném NotFoundException (404)")
        void getById_notFound_throwsNotFound() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(userId, companyId))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );
        }

        @Test
        @DisplayName("Company thuộc user khác → ném NotFoundException")
        void getById_wrongUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(companyRepository.findByIdAndUserId(companyId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(otherUserId, companyId))
                    .isInstanceOf(AppException.NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private UpdateCompanyRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateCompanyRequest(
                    "Google LLC", "https://google.com",
                    "Technology", CompanySize.ENTERPRISE,
                    "Hanoi", true, "Updated notes"
            );
        }

        @Test
        @DisplayName("Happy path: cập nhật thành công → trả về CompanyResponse mới")
        void update_success_returnsUpdatedResponse() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(companyRepository.existsByUserIdAndNameExcluding(userId, "Google LLC", companyId))
                    .willReturn(false);
            given(companyRepository.save(any(Company.class))).willReturn(mockCompany);

            CompanyResponse result = service.update(userId, companyId, request);

            assertThat(result).isNotNull();
            then(companyRepository).should().save(mockCompany);
        }

        @Test
        @DisplayName("Cập nhật thành công → các field được set đúng")
        void update_success_setsAllFields() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(companyRepository.existsByUserIdAndNameExcluding(userId, "Google LLC", companyId))
                    .willReturn(false);
            given(companyRepository.save(any(Company.class))).willReturn(mockCompany);

            service.update(userId, companyId, request);

            assertThat(mockCompany.getName()).isEqualTo("Google LLC");
            assertThat(mockCompany.getLocation()).isEqualTo("Hanoi");
            assertThat(mockCompany.getIsOutsource()).isTrue();
            assertThat(mockCompany.getNotes()).isEqualTo("Updated notes");
        }

        @Test
        @DisplayName("Tên mới trùng với company khác của user → ném ConflictException (409)")
        void update_duplicateName_throwsConflict() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(companyRepository.existsByUserIdAndNameExcluding(userId, "Google LLC", companyId))
                    .willReturn(true);

            assertThatThrownBy(() -> service.update(userId, companyId, request))
                    .isInstanceOf(AppException.ConflictException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(409)
                    );

            then(companyRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Tên không đổi → không bị conflict (exclude chính nó)")
        void update_sameName_doesNotConflict() {
            UpdateCompanyRequest sameNameRequest = new UpdateCompanyRequest(
                    "Google", "https://google.com",
                    "Technology", CompanySize.ENTERPRISE,
                    "HCM", false, "Same name"
            );
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(companyRepository.existsByUserIdAndNameExcluding(userId, "Google", companyId))
                    .willReturn(false);
            given(companyRepository.save(any(Company.class))).willReturn(mockCompany);

            assertThatCode(() -> service.update(userId, companyId, sameNameRequest))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Company không tồn tại → ném NotFoundException")
        void update_companyNotFound_throwsNotFound() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(userId, companyId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(companyRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("size null → default UNKNOWN khi update")
        void update_nullSize_defaultsToUnknown() {
            UpdateCompanyRequest nullSizeRequest = new UpdateCompanyRequest(
                    "Google", null, null, null, null, null, null
            );
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));
            given(companyRepository.existsByUserIdAndNameExcluding(userId, "Google", companyId))
                    .willReturn(false);
            given(companyRepository.save(any(Company.class))).willReturn(mockCompany);

            service.update(userId, companyId, nullSizeRequest);

            assertThat(mockCompany.getSize()).isEqualTo(CompanySize.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Happy path: xóa thành công → company bị soft delete")
        void delete_success_softDeletesCompany() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));

            service.delete(userId, companyId);

            assertThat(mockCompany.isDeleted()).isTrue();
            assertThat(mockCompany.getDeletedAt()).isNotNull();
            then(companyRepository).should().save(mockCompany);
        }

        @Test
        @DisplayName("Company không tồn tại → ném NotFoundException, không soft delete")
        void delete_companyNotFound_throwsNotFound() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(userId, companyId))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(companyRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Company thuộc user khác → ném NotFoundException, không soft delete")
        void delete_wrongUser_throwsNotFound() {
            UUID otherUserId = UUID.randomUUID();
            given(companyRepository.findByIdAndUserId(companyId, otherUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(otherUserId, companyId))
                    .isInstanceOf(AppException.NotFoundException.class);

            assertThat(mockCompany.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("toResponse() — mapping kiểm tra")
    class ToResponseTests {

        @Test
        @DisplayName("Tất cả field được map đúng sang CompanyResponse")
        void toResponse_mapsAllFieldsCorrectly() {
            given(companyRepository.findByIdAndUserId(companyId, userId))
                    .willReturn(Optional.of(mockCompany));

            CompanyResponse result = service.getById(userId, companyId);

            assertThat(result.id()).isEqualTo(companyId);
            assertThat(result.name()).isEqualTo("Google");
            assertThat(result.website()).isEqualTo("https://google.com");
            assertThat(result.industry()).isEqualTo("Technology");
            assertThat(result.size()).isEqualTo(CompanySize.ENTERPRISE);
            assertThat(result.location()).isEqualTo("Ho Chi Minh City");
            assertThat(result.isOutsource()).isFalse();
            assertThat(result.notes()).isEqualTo("Top company");
            assertThat(result.createdAt()).isNotNull();
            assertThat(result.updatedAt()).isNotNull();
        }
    }
}