package com.jobhunt.tracker.module.auth.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.config.mail.EmailService;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.auth.dto.*;
import com.jobhunt.tracker.module.auth.entity.OtpToken;
import com.jobhunt.tracker.module.auth.entity.RefreshToken;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.auth.repository.OtpTokenRepository;
import com.jobhunt.tracker.module.auth.repository.RefreshTokenRepository;
import com.jobhunt.tracker.module.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private OtpTokenRepository otpTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final String fullName = "Nguyen Van A";
    private final String rawPassword = "Secret@123";
    private final String encodedPassword = "$2a$10$hashedpassword";
    private final String accessToken = "mock.access.token";
    private final String refreshTokenValue = "mock-refresh-token-uuid";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);

        mockUser = User.builder()
                .email(email)
                .passwordHash(encodedPassword)
                .fullName(fullName)
                .build();
        ReflectionTestUtils.setField(mockUser, "id", userId);
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private RegisterRequest request;

        @BeforeEach
        void setUp() {
            request = new RegisterRequest(email, rawPassword, fullName);
        }

        @Test
        @DisplayName("Happy path: đăng ký thành công → trả về AuthResult có accessToken")
        void register_success_returnsAuthResult() {
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willReturn(mockUser);
            given(jwtService.generateToken(any(), eq(email))).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900000L);
            willDoNothing().given(emailService)
                    .sendVerificationEmail(anyString(), anyString(), anyString());

            AuthResult result = authService.register(request);

            assertThat(result).isNotNull();
            assertThat(result.authResponse().accessToken()).isEqualTo(accessToken);
            assertThat(result.authResponse().email()).isEqualTo(email);
            assertThat(result.authResponse().tokenType()).isEqualTo("Bearer");
            assertThat(result.refreshToken()).isNotNull();
        }

        @Test
        @DisplayName("Email đã tồn tại → ném AppException.ConflictException (409)")
        void register_emailAlreadyExists_throwsConflict() {
            given(userRepository.existsByEmail(email)).willReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(AppException.ConflictException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(409);
                        assertThat(appEx.getErrorCode()).isEqualTo("CONFLICT");
                    });

            then(userRepository).should(never()).save(any());
            then(emailService).should(never()).sendVerificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Đăng ký thành công → gửi verification email đúng địa chỉ")
        void register_success_sendsVerificationEmail() {
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn(encodedPassword);
            given(userRepository.save(any())).willReturn(mockUser);
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900000L);

            authService.register(request);

            then(emailService).should().sendVerificationEmail(
                    eq(email),
                    eq(fullName),
                    anyString()
            );
        }

        @Test
        @DisplayName("Đăng ký thành công → OTP lưu với type EMAIL_VERIFICATION và expiry 24h")
        void register_success_savesOtpTokenWithCorrectTypeAndExpiry() {
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn(encodedPassword);
            given(userRepository.save(any())).willReturn(mockUser);
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900000L);

            authService.register(request);

            ArgumentCaptor<OtpToken> otpCaptor = ArgumentCaptor.forClass(OtpToken.class);
            then(otpTokenRepository).should().save(otpCaptor.capture());

            OtpToken savedOtp = otpCaptor.getValue();
            assertThat(savedOtp.getType()).isEqualTo(OtpToken.OtpType.EMAIL_VERIFICATION);
            assertThat(savedOtp.getIsUsed()).isFalse();
            assertThat(savedOtp.getExpiresAt())
                    .isAfter(LocalDateTime.now().plusHours(23))
                    .isBefore(LocalDateTime.now().plusHours(25));
        }

        @Test
        @DisplayName("Đăng ký thành công → password được encode trước khi lưu")
        void register_success_encodesPassword() {
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
            given(userRepository.save(any())).willReturn(mockUser);
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900000L);

            authService.register(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(encodedPassword);
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        private LoginRequest request;

        @BeforeEach
        void setUp() {
            request = new LoginRequest(email, rawPassword);
        }

        @Test
        @DisplayName("Happy path: đăng nhập thành công → trả về AuthResult")
        void login_success_returnsAuthResult() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(jwtService.generateToken(userId, email)).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900000L);

            AuthResult result = authService.login(request);

            assertThat(result.authResponse().email()).isEqualTo(email);
            assertThat(result.authResponse().accessToken()).isEqualTo(accessToken);
            assertThat(result.authResponse().userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Sai credentials → ném AppException.UnauthorizedException (401)")
        void login_badCredentials_throwsUnauthorized() {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AppException.UnauthorizedException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(401);
                        assertThat(appEx.getErrorCode()).isEqualTo("UNAUTHORIZED");
                    });

            then(userRepository).should(never()).findByEmail(any());
        }

        @Test
        @DisplayName("Đăng nhập thành công → revoke toàn bộ refresh token cũ")
        void login_success_revokesAllOldRefreshTokens() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900000L);

            authService.login(request);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("User không tìm thấy sau auth → ném AppException.NotFoundException (404)")
        void login_userNotFoundAfterAuth_throwsNotFound() {
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getStatus().value()).isEqualTo(404));
        }
    }

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        private RefreshToken mockRefreshToken;

        @BeforeEach
        void setUp() {
            mockRefreshToken = buildValidRefreshToken(mockUser);
        }

        @Test
        @DisplayName("Happy path: token hợp lệ → trả về token pair mới")
        void refresh_validToken_returnsNewTokenPair() {
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(mockRefreshToken));
            given(jwtService.generateToken(userId, email)).willReturn("new.access.token");
            given(jwtService.getExpirationTime()).willReturn(900000L);

            TokenResponse response = authService.refresh(refreshTokenValue);

            assertThat(response.accessToken()).isEqualTo("new.access.token");
            assertThat(response.refreshToken()).isNotNull();
            assertThat(response.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("Token không tồn tại → ném AppException.UnauthorizedException")
        void refresh_tokenNotFound_throwsUnauthorized() {
            given(refreshTokenRepository.findValidToken(anyString()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("invalid-token"))
                    .isInstanceOf(AppException.UnauthorizedException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getStatus().value()).isEqualTo(401));
        }

        @Test
        @DisplayName("Token đã bị revoke → reuse detected → throw 401 + revoke all sessions")
        void refresh_revokedToken_throwsUnauthorized() {

            RefreshToken revokedToken = buildRevokedRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.empty());
            given(refreshTokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refresh(refreshTokenValue))
                    .isInstanceOf(AppException.UnauthorizedException.class);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Refresh thành công → token cũ bị revoke (Rotation pattern)")
        void refresh_success_revokesOldToken() {
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(mockRefreshToken));
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900000L);

            authService.refresh(refreshTokenValue);

            assertThat(mockRefreshToken.getIsRevoked()).isTrue();
            then(refreshTokenRepository).should(atLeastOnce()).save(mockRefreshToken);
        }

        @Test
        @DisplayName("Refresh thành công → refresh token mới được lưu vào DB")
        void refresh_success_savesNewRefreshToken() {
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(mockRefreshToken));
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900000L);

            authService.refresh(refreshTokenValue);

            then(refreshTokenRepository).should(times(2)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Token đã revoke bị dùng lại → revoke ALL session của user (Reuse Detection)")
        void refresh_reuseDetected_revokesAllUserSessions() {
            RefreshToken revokedToken = buildRevokedRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.empty());
            given(refreshTokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refresh(refreshTokenValue))
                    .isInstanceOf(AppException.UnauthorizedException.class);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Token không tồn tại trong DB → throw 401, không revoke gì cả")
        void refresh_tokenNotExistAtAll_throwsUnauthorizedWithoutRevoking() {
            given(refreshTokenRepository.findValidToken(anyString()))
                    .willReturn(Optional.empty());
            given(refreshTokenRepository.findByToken(anyString()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("ghost-token"))
                    .isInstanceOf(AppException.UnauthorizedException.class);

            then(refreshTokenRepository).should(never()).revokeAllByUserId(any());
        }
    }

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("Happy path: logout thành công → token bị revoke")
        void logout_validToken_revokesToken() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));

            authService.logout(refreshTokenValue);

            assertThat(token.getIsRevoked()).isTrue();
            then(refreshTokenRepository).should().save(token);
        }

        @Test
        @DisplayName("Token không tồn tại → silent, không throw exception")
        void logout_tokenNotFound_doesNothing() {
            given(refreshTokenRepository.findValidToken(anyString()))
                    .willReturn(Optional.empty());

            assertThatCode(() -> authService.logout("ghost-token"))
                    .doesNotThrowAnyException();

            then(refreshTokenRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("logoutAll()")
    class LogoutAllTests {

        @Test
        @DisplayName("Happy path: logout all → revokeAllByUserId được gọi")
        void logoutAll_validToken_revokesAllUserTokens() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));

            authService.logoutAll(refreshTokenValue);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
            then(refreshTokenRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Token không tồn tại → silent, không throw exception")
        void logoutAll_tokenNotFound_doesNothing() {
            given(refreshTokenRepository.findValidToken(anyString()))
                    .willReturn(Optional.empty());

            assertThatCode(() -> authService.logoutAll("ghost-token"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmailTests {

        private OtpToken mockOtp;

        @BeforeEach
        void setUp() {
            mockOtp = buildValidOtp(OtpToken.OtpType.EMAIL_VERIFICATION, mockUser);
        }

        @Test
        @DisplayName("Happy path: token hợp lệ → user.isVerified = true")
        void verifyEmail_validToken_setsUserVerified() {
            given(otpTokenRepository.findValidToken(
                    "valid-otp", OtpToken.OtpType.EMAIL_VERIFICATION))
                    .willReturn(Optional.of(mockOtp));

            authService.verifyEmail("valid-otp");

            assertThat(mockUser.getIsVerified()).isTrue();
            then(userRepository).should().save(mockUser);
        }

        @Test
        @DisplayName("Token hợp lệ → OTP.isUsed = true sau khi verify")
        void verifyEmail_validToken_marksOtpAsUsed() {
            given(otpTokenRepository.findValidToken(
                    "valid-otp", OtpToken.OtpType.EMAIL_VERIFICATION))
                    .willReturn(Optional.of(mockOtp));

            authService.verifyEmail("valid-otp");

            assertThat(mockOtp.getIsUsed()).isTrue();
            then(otpTokenRepository).should().save(mockOtp);
        }

        @Test
        @DisplayName("Token không tồn tại → ném AppException.BadRequestException (400)")
        void verifyEmail_tokenNotFound_throwsBadRequest() {
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(400);
                        assertThat(appEx.getErrorCode()).isEqualTo("BAD_REQUEST");
                    });
        }

        @Test
        @DisplayName("Token đã sử dụng (isUsed=true) → ném AppException.BadRequestException")
        void verifyEmail_alreadyUsedToken_throwsBadRequest() {
            OtpToken usedOtp = buildUsedOtp(OtpToken.OtpType.EMAIL_VERIFICATION, mockUser);
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.of(usedOtp));

            assertThatThrownBy(() -> authService.verifyEmail("used-token"))
                    .isInstanceOf(AppException.BadRequestException.class);

            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Email tồn tại → gửi reset password email")
        void forgotPassword_emailExists_sendsResetEmail() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));

            authService.forgotPassword(email);

            then(emailService).should().sendResetPasswordEmail(
                    eq(email), eq(fullName), anyString()
            );
        }

        @Test
        @DisplayName("Email không tồn tại → silent (chống user enumeration attack)")
        void forgotPassword_emailNotExists_doesNothingSilently() {
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            authService.forgotPassword("notfound@example.com");

            then(emailService).should(never()).sendResetPasswordEmail(any(), any(), any());
            then(otpTokenRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Email tồn tại → invalidate OTP cũ trước khi tạo mới")
        void forgotPassword_emailExists_invalidatesOldOtpFirst() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));

            authService.forgotPassword(email);

            then(otpTokenRepository).should().invalidateAllByUserIdAndType(
                    userId, OtpToken.OtpType.RESET_PASSWORD
            );
        }

        @Test
        @DisplayName("OTP reset password được tạo với type đúng và expiry 15 phút")
        void forgotPassword_createsResetOtpWithCorrectMetadata() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));

            authService.forgotPassword(email);

            ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
            then(otpTokenRepository).should().save(captor.capture());

            OtpToken saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(OtpToken.OtpType.RESET_PASSWORD);
            assertThat(saved.getIsUsed()).isFalse();
            assertThat(saved.getExpiresAt())
                    .isAfter(LocalDateTime.now().plusMinutes(14))
                    .isBefore(LocalDateTime.now().plusMinutes(16));
        }
    }

    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        private ResetPasswordRequest request;
        private OtpToken mockOtp;

        @BeforeEach
        void setUp() {
            request = new ResetPasswordRequest("valid-reset-token", "NewSecret@123");
            mockOtp = buildValidOtp(OtpToken.OtpType.RESET_PASSWORD, mockUser);
        }

        @Test
        @DisplayName("Happy path: token hợp lệ → password được hash và cập nhật")
        void resetPassword_validToken_updatesPasswordHash() {
            given(otpTokenRepository.findValidToken(
                    "valid-reset-token", OtpToken.OtpType.RESET_PASSWORD))
                    .willReturn(Optional.of(mockOtp));
            given(passwordEncoder.encode("NewSecret@123")).willReturn("new-hashed-password");

            authService.resetPassword(request);

            assertThat(mockUser.getPasswordHash()).isEqualTo("new-hashed-password");
            then(userRepository).should().save(mockUser);
        }

        @Test
        @DisplayName("Reset thành công → toàn bộ refresh token bị revoke (force logout)")
        void resetPassword_success_revokesAllRefreshTokens() {
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.of(mockOtp));
            given(passwordEncoder.encode(any())).willReturn("new-hashed");

            authService.resetPassword(request);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Token không tồn tại → ném AppException.BadRequestException (400)")
        void resetPassword_invalidToken_throwsBadRequest() {
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getStatus().value()).isEqualTo(400));
        }

        @Test
        @DisplayName("Token đã dùng → ném AppException.BadRequestException")
        void resetPassword_usedToken_throwsBadRequest() {
            OtpToken usedOtp = buildUsedOtp(OtpToken.OtpType.RESET_PASSWORD, mockUser);
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.of(usedOtp));

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(AppException.BadRequestException.class);

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Reset thành công → OTP.isUsed = true")
        void resetPassword_success_marksOtpAsUsed() {
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.of(mockOtp));
            given(passwordEncoder.encode(any())).willReturn("new-hashed");

            authService.resetPassword(request);

            assertThat(mockOtp.getIsUsed()).isTrue();
            then(otpTokenRepository).should().save(mockOtp);
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        private ChangePasswordRequest request;

        @BeforeEach
        void setUp() {
            request = new ChangePasswordRequest("Secret@123", "NewSecret@456");
        }

        @Test
        @DisplayName("Happy path: đổi mật khẩu thành công → password được cập nhật")
        void changePassword_success_updatesPasswordHash() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(true);
            given(passwordEncoder.matches("NewSecret@456", encodedPassword)).willReturn(false);
            given(passwordEncoder.encode("NewSecret@456")).willReturn("new-hashed-password");

            authService.changePassword(email, request);

            assertThat(mockUser.getPasswordHash()).isEqualTo("new-hashed-password");
            then(userRepository).should().save(mockUser);
        }

        @Test
        @DisplayName("Đổi mật khẩu thành công → revoke toàn bộ refresh token (force logout)")
        void changePassword_success_revokesAllRefreshTokens() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(true);
            given(passwordEncoder.matches("NewSecret@456", encodedPassword)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("new-hashed");

            authService.changePassword(email, request);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Mật khẩu hiện tại sai → ném AppException.BadRequestException (400)")
        void changePassword_wrongCurrentPassword_throwsBadRequest() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(false);

            assertThatThrownBy(() -> authService.changePassword(email, request))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(400);
                        assertThat(appEx.getMessage()).contains("Current password is incorrect");
                    });

            then(userRepository).should(never()).save(any());
            then(refreshTokenRepository).should(never()).revokeAllByUserId(any());
        }

        @Test
        @DisplayName("Mật khẩu mới giống mật khẩu cũ → ném AppException.BadRequestException (400)")
        void changePassword_sameAsCurrentPassword_throwsBadRequest() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(true);
            given(passwordEncoder.matches("NewSecret@456", encodedPassword)).willReturn(true);

            assertThatThrownBy(() -> authService.changePassword(email, request))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getMessage()).contains("must be different");
                    });

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("User không tồn tại → ném AppException.NotFoundException (404)")
        void changePassword_userNotFound_throwsNotFound() {
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(email, request))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getStatus().value()).isEqualTo(404));
        }
    }

    private RefreshToken buildValidRefreshToken(User user) {
        return RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private RefreshToken buildRevokedRefreshToken(User user) {
        RefreshToken token = buildValidRefreshToken(user);
        token.revoke();
        return token;
    }

    private OtpToken buildValidOtp(OtpToken.OtpType type, User user) {
        return OtpToken.builder()
                .token("valid-otp-" + UUID.randomUUID())
                .user(user)
                .type(type)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    private OtpToken buildUsedOtp(OtpToken.OtpType type, User user) {
        OtpToken otp = buildValidOtp(type, user);
        otp.markAsUsed();
        return otp;
    }
}