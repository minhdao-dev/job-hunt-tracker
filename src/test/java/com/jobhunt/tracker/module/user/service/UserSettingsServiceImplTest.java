package com.jobhunt.tracker.module.user.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.config.mail.EmailService;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.config.security.TokenBlacklistService;
import com.jobhunt.tracker.module.auth.entity.OtpToken;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.auth.repository.OtpTokenRepository;
import com.jobhunt.tracker.module.auth.repository.RefreshTokenRepository;
import com.jobhunt.tracker.module.auth.repository.UserRepository;
import com.jobhunt.tracker.module.user.dto.*;
import com.jobhunt.tracker.module.user.entity.UserSettings;
import com.jobhunt.tracker.module.user.entity.WorkType;
import com.jobhunt.tracker.module.user.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSettingsServiceImpl Tests")
class UserSettingsServiceImplTest {

    @Mock private UserRepository         userRepository;
    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private OtpTokenRepository     otpTokenRepository;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private JwtService             jwtService;
    @Mock private TokenBlacklistService  blacklistService;
    @Mock private EmailService           emailService;

    @InjectMocks
    private UserSettingsServiceImpl service;

    private User         mockUser;
    private UserSettings mockSettings;

    private final UUID   userId        = UUID.randomUUID();
    private final String email         = "dao@example.com";
    private final String fullName      = "Nguyen Minh Dao";
    private final String encodedPw     = "$2a$10$hashed";
    private final String rawPw         = "Secret@123";
    private final String accessToken   = "mock.access.token";
    private final String mockJti       = UUID.randomUUID().toString();
    private final long   remainingTtl  = 600_000L;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email(email)
                .passwordHash(encodedPw)
                .fullName(fullName)
                .build();
        ReflectionTestUtils.setField(mockUser, "id", userId);

        mockSettings = UserSettings.builder()
                .user(mockUser)
                .build();
        ReflectionTestUtils.setField(mockSettings, "id", UUID.randomUUID());
    }

    @Nested
    @DisplayName("getSettings()")
    class GetSettingsTests {

        @Test
        @DisplayName("Happy path: settings tồn tại → trả về đủ 3 phần")
        void getSettings_settingsExist_returnsFullResponse() {
            mockSettings.setTargetRole("Java Backend Developer");
            mockSettings.setWorkType(WorkType.HYBRID);
            mockSettings.setReminderEnabled(true);
            mockSettings.setBio("Backend dev");

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            UserSettingsResponse result = service.getSettings(userId);

            assertThat(result).isNotNull();
            assertThat(result.profile().email()).isEqualTo(email);
            assertThat(result.profile().fullName()).isEqualTo(fullName);
            assertThat(result.profile().bio()).isEqualTo("Backend dev");
            assertThat(result.notification().reminderEnabled()).isTrue();
            assertThat(result.preferences().targetRole()).isEqualTo("Java Backend Developer");
            assertThat(result.preferences().workType()).isEqualTo(WorkType.HYBRID);
        }

        @Test
        @DisplayName("Settings chưa tồn tại → tự tạo mới rồi trả về")
        void getSettings_settingsNotExist_createsAndReturns() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.empty());
            given(userSettingsRepository.save(any(UserSettings.class))).willReturn(mockSettings);

            UserSettingsResponse result = service.getSettings(userId);

            assertThat(result).isNotNull();
            then(userSettingsRepository).should().save(any(UserSettings.class));
        }

        @Test
        @DisplayName("User không tồn tại → ném NotFoundException (404)")
        void getSettings_userNotFound_throwsNotFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSettings(userId))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );
        }

        @Test
        @DisplayName("User đã bị soft delete → ném NotFoundException (404)")
        void getSettings_deletedUser_throwsNotFound() {
            mockUser.softDelete();
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> service.getSettings(userId))
                    .isInstanceOf(AppException.NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {

        private UpdateProfileRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateProfileRequest(
                    "Dao Nguyen", "https://avatar.com/img.png", "Backend dev"
            );
        }

        @Test
        @DisplayName("Happy path: cập nhật fullName, avatarUrl trong User + bio trong Settings")
        void updateProfile_success_updatesUserAndSettings() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            service.updateProfile(userId, request);

            assertThat(mockUser.getFullName()).isEqualTo("Dao Nguyen");
            assertThat(mockUser.getAvatarUrl()).isEqualTo("https://avatar.com/img.png");
            assertThat(mockSettings.getBio()).isEqualTo("Backend dev");

            then(userRepository).should().save(mockUser);
            then(userSettingsRepository).should().save(mockSettings);
        }

        @Test
        @DisplayName("Settings chưa tồn tại → tự tạo mới trước khi update")
        void updateProfile_settingsNotExist_createsSettings() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.empty());
            given(userSettingsRepository.save(any(UserSettings.class))).willReturn(mockSettings);

            service.updateProfile(userId, request);

            then(userSettingsRepository).should(times(2)).save(any(UserSettings.class));
        }

        @Test
        @DisplayName("User không tồn tại → ném NotFoundException")
        void updateProfile_userNotFound_throwsNotFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateProfile(userId, request))
                    .isInstanceOf(AppException.NotFoundException.class);

            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateNotification()")
    class UpdateNotificationTests {

        @Test
        @DisplayName("Happy path: cập nhật đầy đủ các field notification")
        void updateNotification_success_updatesAllFields() {
            UpdateNotificationRequest request = new UpdateNotificationRequest(
                    false, 14, true, "America/New_York", "en"
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            service.updateNotification(userId, request);

            assertThat(mockSettings.getReminderEnabled()).isFalse();
            assertThat(mockSettings.getReminderAfterDays()).isEqualTo(14);
            assertThat(mockSettings.getEmailNotifications()).isTrue();
            assertThat(mockSettings.getTimezone()).isEqualTo("America/New_York");
            assertThat(mockSettings.getLanguage()).isEqualTo("en");

            then(userSettingsRepository).should().save(mockSettings);
        }

        @Test
        @DisplayName("timezone null → giữ nguyên timezone cũ")
        void updateNotification_nullTimezone_keepsExistingTimezone() {
            mockSettings.setTimezone("Asia/Ho_Chi_Minh");
            UpdateNotificationRequest request = new UpdateNotificationRequest(
                    true, 7, true, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            service.updateNotification(userId, request);

            assertThat(mockSettings.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
        }

        @Test
        @DisplayName("language null → giữ nguyên language cũ")
        void updateNotification_nullLanguage_keepsExistingLanguage() {
            mockSettings.setLanguage("vi");
            UpdateNotificationRequest request = new UpdateNotificationRequest(
                    true, 7, true, "Asia/Bangkok", null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            service.updateNotification(userId, request);

            assertThat(mockSettings.getLanguage()).isEqualTo("vi");
        }
    }

    @Nested
    @DisplayName("updatePreferences()")
    class UpdatePreferencesTests {

        @Test
        @DisplayName("Happy path: cập nhật đầy đủ job preferences")
        void updatePreferences_success_updatesAllFields() {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                    "Java Backend Developer", 15, 25,
                    "Ho Chi Minh City", WorkType.HYBRID
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            service.updatePreferences(userId, request);

            assertThat(mockSettings.getTargetRole()).isEqualTo("Java Backend Developer");
            assertThat(mockSettings.getTargetSalaryMin()).isEqualTo(15);
            assertThat(mockSettings.getTargetSalaryMax()).isEqualTo(25);
            assertThat(mockSettings.getPreferredLocation()).isEqualTo("Ho Chi Minh City");
            assertThat(mockSettings.getWorkType()).isEqualTo(WorkType.HYBRID);

            then(userSettingsRepository).should().save(mockSettings);
        }

        @Test
        @DisplayName("salaryMin > salaryMax → ném BadRequestException (400)")
        void updatePreferences_salaryMinExceedsMax_throwsBadRequest() {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                    "Java Backend", 30, 20,
                    "HCM", WorkType.REMOTE
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            assertThatThrownBy(() -> service.updatePreferences(userId, request))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(400);
                        assertThat(appEx.getMessage()).contains("Salary min must not exceed salary max");
                    });

            then(userSettingsRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("salaryMin = salaryMax → hợp lệ, không throw")
        void updatePreferences_salaryMinEqualsMax_isValid() {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                    "Java Backend", 20, 20, "HCM", WorkType.ONSITE
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            assertThatCode(() -> service.updatePreferences(userId, request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("salaryMin null → bỏ qua validate salary range")
        void updatePreferences_nullSalaryMin_skipsValidation() {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                    "Java Backend", null, 20, "HCM", WorkType.HYBRID
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            assertThatCode(() -> service.updatePreferences(userId, request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("salaryMax null → bỏ qua validate salary range")
        void updatePreferences_nullSalaryMax_skipsValidation() {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                    "Java Backend", 20, null, "HCM", WorkType.HYBRID
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            assertThatCode(() -> service.updatePreferences(userId, request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("workType null → set null vào settings (xóa preference)")
        void updatePreferences_nullWorkType_setsNullInSettings() {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                    "Java Backend", null, null, null, null
            );
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            service.updatePreferences(userId, request);

            assertThat(mockSettings.getWorkType()).isNull();
        }
    }

    @Nested
    @DisplayName("changeEmail()")
    class ChangeEmailTests {

        private ChangeEmailRequest request;

        @BeforeEach
        void setUp() {
            request = new ChangeEmailRequest("new@example.com", rawPw);
        }

        @Test
        @DisplayName("Happy path: đổi email thành công → pendingEmail được set, OTP tạo, email gửi đi")
        void changeEmail_success_setsPendingEmailAndSendsVerification() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(true);
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);
            given(jwtService.isTokenValid(accessToken)).willReturn(true);
            given(jwtService.extractJti(accessToken)).willReturn(mockJti);
            given(jwtService.getRemainingTtlMillis(accessToken)).willReturn(remainingTtl);

            service.changeEmail(userId, request, accessToken);

            assertThat(mockUser.getPendingEmail()).isEqualTo("new@example.com");
            then(userRepository).should().save(mockUser);
            then(otpTokenRepository).should().save(any(OtpToken.class));
            then(emailService).should().sendVerificationEmail(
                    eq("new@example.com"), eq(fullName), anyString()
            );
            then(refreshTokenRepository).should().revokeAllByUserId(userId);
            then(blacklistService).should().blacklist(mockJti, remainingTtl);
        }

        @Test
        @DisplayName("OTP type EMAIL_VERIFICATION được tạo với expiry ~24h")
        void changeEmail_success_createsOtpWithCorrectMetadata() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(true);
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);
            given(jwtService.isTokenValid(accessToken)).willReturn(false);

            service.changeEmail(userId, request, accessToken);

            ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
            then(otpTokenRepository).should().save(captor.capture());

            OtpToken saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(OtpToken.OtpType.EMAIL_VERIFICATION);
            assertThat(saved.getIsUsed()).isFalse();
            assertThat(saved.getExpiresAt())
                    .isAfter(java.time.LocalDateTime.now().plusHours(23))
                    .isBefore(java.time.LocalDateTime.now().plusHours(25));
        }

        @Test
        @DisplayName("Mật khẩu sai → ném BadRequestException (400)")
        void changeEmail_wrongPassword_throwsBadRequest() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(false);

            assertThatThrownBy(() -> service.changeEmail(userId, request, accessToken))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex ->
                            assertThat(ex.getMessage()).contains("Current password is incorrect")
                    );

            then(userRepository).should(never()).save(any());
            then(emailService).should(never()).sendVerificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Email mới giống email hiện tại → ném BadRequestException")
        void changeEmail_sameAsCurrentEmail_throwsBadRequest() {
            ChangeEmailRequest sameEmailRequest = new ChangeEmailRequest(email, rawPw);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(true);

            assertThatThrownBy(() -> service.changeEmail(userId, sameEmailRequest, accessToken))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex ->
                            assertThat(ex.getMessage()).contains("must be different")
                    );
        }

        @Test
        @DisplayName("Email mới đã tồn tại trong hệ thống → ném ConflictException (409)")
        void changeEmail_emailAlreadyInUse_throwsConflict() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(true);
            given(userRepository.existsByEmail("new@example.com")).willReturn(true);

            assertThatThrownBy(() -> service.changeEmail(userId, request, accessToken))
                    .isInstanceOf(AppException.ConflictException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(409)
                    );

            then(otpTokenRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Access token null → bỏ qua blacklist, không throw")
        void changeEmail_nullAccessToken_skipsBlacklist() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(true);
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);

            assertThatCode(() -> service.changeEmail(userId, request, null))
                    .doesNotThrowAnyException();

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccountTests {

        private DeleteAccountRequest request;

        @BeforeEach
        void setUp() {
            request = new DeleteAccountRequest(rawPw);
        }

        @Test
        @DisplayName("Happy path: xóa tài khoản thành công → soft delete + revoke all + blacklist")
        void deleteAccount_success_softDeletesAndRevokesAll() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(true);
            given(jwtService.isTokenValid(accessToken)).willReturn(true);
            given(jwtService.extractJti(accessToken)).willReturn(mockJti);
            given(jwtService.getRemainingTtlMillis(accessToken)).willReturn(remainingTtl);

            service.deleteAccount(userId, request, accessToken);

            assertThat(mockUser.isDeleted()).isTrue();
            then(userRepository).should().save(mockUser);
            then(refreshTokenRepository).should().revokeAllByUserId(userId);
            then(blacklistService).should().blacklist(mockJti, remainingTtl);
        }

        @Test
        @DisplayName("Sau khi soft delete → user.deletedAt không null")
        void deleteAccount_success_setsDeletedAt() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(true);
            given(jwtService.isTokenValid(accessToken)).willReturn(false);

            service.deleteAccount(userId, request, accessToken);

            assertThat(mockUser.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Mật khẩu sai → ném BadRequestException, không xóa")
        void deleteAccount_wrongPassword_throwsBadRequest() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(false);

            assertThatThrownBy(() -> service.deleteAccount(userId, request, accessToken))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex ->
                            assertThat(ex.getMessage()).contains("Current password is incorrect")
                    );

            assertThat(mockUser.isDeleted()).isFalse();
            then(userRepository).should(never()).save(any());
            then(refreshTokenRepository).should(never()).revokeAllByUserId(any());
            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("User không tồn tại → ném NotFoundException")
        void deleteAccount_userNotFound_throwsNotFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAccount(userId, request, accessToken))
                    .isInstanceOf(AppException.NotFoundException.class);
        }

        @Test
        @DisplayName("Access token null → bỏ qua blacklist, vẫn soft delete bình thường")
        void deleteAccount_nullAccessToken_stillSoftDeletes() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPw, encodedPw)).willReturn(true);

            service.deleteAccount(userId, request, null);

            assertThat(mockUser.isDeleted()).isTrue();
            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("findOrCreateSettings() — edge cases")
    class FindOrCreateSettingsTests {

        @Test
        @DisplayName("Settings tồn tại → không tạo mới, dùng cái có sẵn")
        void findOrCreate_settingsExist_doesNotCreateNew() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.of(mockSettings));

            service.getSettings(userId);

            then(userSettingsRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Settings chưa tồn tại → tạo mới với default values")
        void findOrCreate_settingsNotExist_createsWithDefaults() {
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userSettingsRepository.findByUserId(userId)).willReturn(Optional.empty());

            ArgumentCaptor<UserSettings> captor = ArgumentCaptor.forClass(UserSettings.class);
            given(userSettingsRepository.save(captor.capture())).willReturn(mockSettings);

            service.getSettings(userId);

            UserSettings created = captor.getValue();
            assertThat(created.getUser()).isEqualTo(mockUser);
            assertThat(created.getReminderEnabled()).isTrue();
            assertThat(created.getReminderAfterDays()).isEqualTo(7);
            assertThat(created.getEmailNotifications()).isTrue();
            assertThat(created.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
            assertThat(created.getLanguage()).isEqualTo("vi");
        }
    }
}