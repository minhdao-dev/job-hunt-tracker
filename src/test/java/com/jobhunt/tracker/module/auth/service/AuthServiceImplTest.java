package com.jobhunt.tracker.module.auth.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.config.mail.EmailService;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.config.security.TokenBlacklistService;
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
    @Mock
    private TokenBlacklistService blacklistService;

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
    private final String mockJti = UUID.randomUUID().toString();
    private final long remainingTtl = 600_000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604_800_000L);

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

//        @Test
//        @DisplayName("Happy path: đăng ký thành công → trả về AuthResult có accessToken")
//        void register_success_returnsAuthResult() {
//            given(userRepository.existsByEmail(email)).willReturn(false);
//            given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
//            given(userRepository.save(any(User.class))).willReturn(mockUser);
//            given(jwtService.generateToken(any(), eq(email))).willReturn(accessToken);
//            given(jwtService.getExpirationTime()).willReturn(900_000L);
//
//            AuthResult result = authService.register(request);
//
//            assertThat(result).isNotNull();
//            assertThat(result.authResponse().accessToken()).isEqualTo(accessToken);
//            assertThat(result.authResponse().email()).isEqualTo(email);
//            assertThat(result.authResponse().tokenType()).isEqualTo("Bearer");
//            assertThat(result.refreshToken()).isNotNull();
//        }

        @Test
        @DisplayName("Email đã tồn tại → ném ConflictException (409)")
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
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            authService.register(request);

            then(emailService).should()
                    .sendVerificationEmail(eq(email), eq(fullName), anyString());
        }

        @Test
        @DisplayName("Đăng ký thành công → OTP có type EMAIL_VERIFICATION + expiry ~24h")
        void register_success_savesOtpWithCorrectMetadata() {
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn(encodedPassword);
            given(userRepository.save(any())).willReturn(mockUser);
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            authService.register(request);

            ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
            then(otpTokenRepository).should().save(captor.capture());

            OtpToken saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(OtpToken.OtpType.EMAIL_VERIFICATION);
            assertThat(saved.getIsUsed()).isFalse();
            assertThat(saved.getExpiresAt())
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
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo(encodedPassword);
        }

        @Test
        @DisplayName("Đăng ký thành công → KHÔNG gọi blacklist")
        void register_success_neverCallsBlacklist() {
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn(encodedPassword);
            given(userRepository.save(any())).willReturn(mockUser);
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            authService.register(request);

            then(blacklistService).should(never()).blacklist(any(), anyLong());
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
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            AuthResult result = authService.login(request);

            assertThat(result.authResponse().email()).isEqualTo(email);
            assertThat(result.authResponse().accessToken()).isEqualTo(accessToken);
            assertThat(result.authResponse().userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Sai credentials → ném UnauthorizedException (401)")
        void login_badCredentials_throwsUnauthorized() {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AppException.UnauthorizedException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(401)
                    );

            then(userRepository).should(never()).findByEmail(any());
        }

        @Test
        @DisplayName("Đăng nhập thành công → revoke toàn bộ refresh token cũ")
        void login_success_revokesAllOldRefreshTokens() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            authService.login(request);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("User không tồn tại sau authenticate → ném NotFoundException (404)")
        void login_userNotFoundAfterAuth_throwsNotFound() {
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );
        }

        @Test
        @DisplayName("Login thành công → KHÔNG gọi blacklist")
        void login_success_neverCallsBlacklist() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            authService.login(request);

            then(blacklistService).should(never()).blacklist(any(), anyLong());
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
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            TokenResponse response = authService.refresh(refreshTokenValue);

            assertThat(response.accessToken()).isEqualTo("new.access.token");
            assertThat(response.refreshToken()).isNotNull();
            assertThat(response.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("Refresh thành công → token cũ bị revoke (Rotation pattern)")
        void refresh_success_revokesOldToken() {
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(mockRefreshToken));
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900_000L);

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
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            authService.refresh(refreshTokenValue);

            then(refreshTokenRepository).should(times(2)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Token đã revoke bị dùng lại → Reuse Detection → revoke ALL sessions + throw 401")
        void refresh_reuseDetected_revokesAllSessionsAndThrows() {
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
        void refresh_tokenNotExistAtAll_throwsWithoutRevoking() {
            given(refreshTokenRepository.findValidToken(anyString()))
                    .willReturn(Optional.empty());
            given(refreshTokenRepository.findByToken(anyString()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("ghost-token"))
                    .isInstanceOf(AppException.UnauthorizedException.class);

            then(refreshTokenRepository).should(never()).revokeAllByUserId(any());
        }

        @Test
        @DisplayName("Refresh → KHÔNG gọi blacklist")
        void refresh_neverCallsBlacklist() {
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(mockRefreshToken));
            given(jwtService.generateToken(any(), any())).willReturn(accessToken);
            given(jwtService.getExpirationTime()).willReturn(900_000L);

            authService.refresh(refreshTokenValue);

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("Happy path: logout thành công → refresh token bị revoke")
        void logout_validToken_revokesRefreshToken() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));

            authService.logout(refreshTokenValue, null);

            assertThat(token.getIsRevoked()).isTrue();
            then(refreshTokenRepository).should().save(token);
        }

        @Test
        @DisplayName("Có access token hợp lệ → access token bị blacklist sau logout")
        void logout_withValidAccessToken_blacklistsAccessToken() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));
            given(jwtService.isTokenValid(accessToken)).willReturn(true);
            given(jwtService.extractJti(accessToken)).willReturn(mockJti);
            given(jwtService.getRemainingTtlMillis(accessToken)).willReturn(remainingTtl);

            authService.logout(refreshTokenValue, accessToken);

            then(blacklistService).should().blacklist(mockJti, remainingTtl);
        }

        @Test
        @DisplayName("Access token null → bỏ qua blacklist, không throw exception")
        void logout_withNullAccessToken_skipsBlacklist() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));

            assertThatCode(() -> authService.logout(refreshTokenValue, null))
                    .doesNotThrowAnyException();

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("Access token blank → bỏ qua blacklist")
        void logout_withBlankAccessToken_skipsBlacklist() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));

            assertThatCode(() -> authService.logout(refreshTokenValue, "   "))
                    .doesNotThrowAnyException();

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("Access token đã hết hạn → bỏ qua blacklist")
        void logout_withExpiredAccessToken_skipsBlacklist() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));
            given(jwtService.isTokenValid(accessToken)).willReturn(false);

            authService.logout(refreshTokenValue, accessToken);

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("Refresh token không tồn tại → silent, không throw, không blacklist")
        void logout_refreshTokenNotFound_doesNothingSilently() {
            given(refreshTokenRepository.findValidToken(anyString()))
                    .willReturn(Optional.empty());

            assertThatCode(() -> authService.logout("ghost-token", null))
                    .doesNotThrowAnyException();

            then(refreshTokenRepository).should(never()).save(any());
            then(blacklistService).should(never()).blacklist(any(), anyLong());
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

            authService.logoutAll(refreshTokenValue, null);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Có access token hợp lệ → access token bị blacklist sau logout all")
        void logoutAll_withValidAccessToken_blacklistsAccessToken() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));
            given(jwtService.isTokenValid(accessToken)).willReturn(true);
            given(jwtService.extractJti(accessToken)).willReturn(mockJti);
            given(jwtService.getRemainingTtlMillis(accessToken)).willReturn(remainingTtl);

            authService.logoutAll(refreshTokenValue, accessToken);

            then(blacklistService).should().blacklist(mockJti, remainingTtl);
        }

        @Test
        @DisplayName("Access token null → bỏ qua blacklist")
        void logoutAll_withNullAccessToken_skipsBlacklist() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));

            assertThatCode(() -> authService.logoutAll(refreshTokenValue, null))
                    .doesNotThrowAnyException();

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("Refresh token không tồn tại → silent, không throw, không blacklist")
        void logoutAll_refreshTokenNotFound_doesNothingSilently() {
            given(refreshTokenRepository.findValidToken(anyString()))
                    .willReturn(Optional.empty());

            assertThatCode(() -> authService.logoutAll("ghost-token", null))
                    .doesNotThrowAnyException();

            then(blacklistService).should(never()).blacklist(any(), anyLong());
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
        @DisplayName("Token hợp lệ → user.isVerified = true")
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
        @DisplayName("Token không tồn tại → ném BadRequestException (400)")
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
        @DisplayName("Token đã dùng (isUsed=true) → ném BadRequestException")
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

            then(emailService).should()
                    .sendResetPasswordEmail(eq(email), eq(fullName), anyString());
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
        @DisplayName("OTP được tạo với type RESET_PASSWORD + expiry ~15 phút")
        void forgotPassword_createsOtpWithCorrectMetadata() {
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
        @DisplayName("Token hợp lệ → password được hash và cập nhật")
        void resetPassword_validToken_updatesPasswordHash() {
            given(otpTokenRepository.findValidToken(
                    "valid-reset-token", OtpToken.OtpType.RESET_PASSWORD))
                    .willReturn(Optional.of(mockOtp));
            given(passwordEncoder.encode("NewSecret@123")).willReturn("new-hashed");

            authService.resetPassword(request);

            assertThat(mockUser.getPasswordHash()).isEqualTo("new-hashed");
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
        @DisplayName("Reset thành công → OTP.isUsed = true")
        void resetPassword_success_marksOtpAsUsed() {
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.of(mockOtp));
            given(passwordEncoder.encode(any())).willReturn("new-hashed");

            authService.resetPassword(request);

            assertThat(mockOtp.getIsUsed()).isTrue();
            then(otpTokenRepository).should().save(mockOtp);
        }

        @Test
        @DisplayName("Token không tồn tại → ném BadRequestException (400)")
        void resetPassword_invalidToken_throwsBadRequest() {
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(400)
                    );
        }

        @Test
        @DisplayName("Token đã dùng → ném BadRequestException, không update password")
        void resetPassword_usedToken_throwsBadRequest() {
            OtpToken usedOtp = buildUsedOtp(OtpToken.OtpType.RESET_PASSWORD, mockUser);
            given(otpTokenRepository.findValidToken(anyString(), any()))
                    .willReturn(Optional.of(usedOtp));

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(AppException.BadRequestException.class);

            then(userRepository).should(never()).save(any());
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
            given(passwordEncoder.encode("NewSecret@456")).willReturn("new-hashed");

            authService.changePassword(email, request, null);

            assertThat(mockUser.getPasswordHash()).isEqualTo("new-hashed");
            then(userRepository).should().save(mockUser);
        }

        @Test
        @DisplayName("Đổi mật khẩu thành công → revoke toàn bộ refresh token")
        void changePassword_success_revokesAllRefreshTokens() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(true);
            given(passwordEncoder.matches("NewSecret@456", encodedPassword)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("new-hashed");

            authService.changePassword(email, request, null);

            then(refreshTokenRepository).should().revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Có access token hợp lệ → access token bị blacklist ngay sau đổi password")
        void changePassword_withValidAccessToken_blacklistsAccessToken() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(true);
            given(passwordEncoder.matches("NewSecret@456", encodedPassword)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("new-hashed");
            given(jwtService.isTokenValid(accessToken)).willReturn(true);
            given(jwtService.extractJti(accessToken)).willReturn(mockJti);
            given(jwtService.getRemainingTtlMillis(accessToken)).willReturn(remainingTtl);

            authService.changePassword(email, request, accessToken);

            then(blacklistService).should().blacklist(mockJti, remainingTtl);
        }

        @Test
        @DisplayName("Access token null → bỏ qua blacklist, không throw")
        void changePassword_withNullAccessToken_skipsBlacklist() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(true);
            given(passwordEncoder.matches("NewSecret@456", encodedPassword)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("new-hashed");

            assertThatCode(() -> authService.changePassword(email, request, null))
                    .doesNotThrowAnyException();

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("Access token đã hết hạn → bỏ qua blacklist")
        void changePassword_withExpiredAccessToken_skipsBlacklist() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(true);
            given(passwordEncoder.matches("NewSecret@456", encodedPassword)).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("new-hashed");
            given(jwtService.isTokenValid(accessToken)).willReturn(false);

            authService.changePassword(email, request, accessToken);

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("Mật khẩu hiện tại sai → ném BadRequestException, không blacklist")
        void changePassword_wrongCurrentPassword_throwsBadRequest() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(false);

            assertThatThrownBy(() -> authService.changePassword(email, request, accessToken))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(400);
                        assertThat(appEx.getMessage()).contains("Current password is incorrect");
                    });

            then(userRepository).should(never()).save(any());
            then(refreshTokenRepository).should(never()).revokeAllByUserId(any());
            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("Mật khẩu mới giống mật khẩu cũ → ném BadRequestException, không blacklist")
        void changePassword_sameAsCurrentPassword_throwsBadRequest() {
            given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("Secret@123", encodedPassword)).willReturn(true);
            given(passwordEncoder.matches("NewSecret@456", encodedPassword)).willReturn(true);

            assertThatThrownBy(() -> authService.changePassword(email, request, accessToken))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .satisfies(ex -> {
                        AppException.BadRequestException appEx = (AppException.BadRequestException) ex;
                        assertThat(appEx.getStatus().value()).isEqualTo(400);
                        assertThat(appEx.getMessage()).contains("must be different"); // ← đúng
                    });

            then(userRepository).should(never()).save(any());
            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }

        @Test
        @DisplayName("User không tồn tại → ném NotFoundException (404), không blacklist")
        void changePassword_userNotFound_throwsNotFound() {
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(email, request, accessToken))
                    .isInstanceOf(AppException.NotFoundException.class)
                    .satisfies(ex ->
                            assertThat(((AppException) ex).getStatus().value()).isEqualTo(404)
                    );

            then(blacklistService).should(never()).blacklist(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("blacklistAccessToken() — edge cases (via logout)")
    class BlacklistAccessTokenEdgeCaseTests {

        @Test
        @DisplayName("Token hợp lệ → blacklist được gọi đúng jti + ttl")
        void blacklist_validToken_callsBlacklistWithCorrectArgs() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));
            given(jwtService.isTokenValid(accessToken)).willReturn(true);
            given(jwtService.extractJti(accessToken)).willReturn(mockJti);
            given(jwtService.getRemainingTtlMillis(accessToken)).willReturn(remainingTtl);

            authService.logout(refreshTokenValue, accessToken);

            ArgumentCaptor<String> jtiCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
            then(blacklistService).should().blacklist(jtiCaptor.capture(), ttlCaptor.capture());

            assertThat(jtiCaptor.getValue()).isEqualTo(mockJti);
            assertThat(ttlCaptor.getValue()).isEqualTo(remainingTtl);
        }

        @Test
        @DisplayName("TTL = 0 → blacklist vẫn được gọi, TokenBlacklistService tự guard")
        void blacklist_ttlZero_stillCallsBlacklist() {
            RefreshToken token = buildValidRefreshToken(mockUser);
            given(refreshTokenRepository.findValidToken(refreshTokenValue))
                    .willReturn(Optional.of(token));
            given(jwtService.isTokenValid(accessToken)).willReturn(true);
            given(jwtService.extractJti(accessToken)).willReturn(mockJti);
            given(jwtService.getRemainingTtlMillis(accessToken)).willReturn(0L);

            authService.logout(refreshTokenValue, accessToken);

            then(blacklistService).should().blacklist(mockJti, 0L);
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