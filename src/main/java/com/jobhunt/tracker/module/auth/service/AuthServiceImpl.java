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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final TokenBlacklistService blacklistService;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw AppException.conflict(
                    "Email already exists: " + request.email()
            );
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .build();

        user = userRepository.save(user);

        String verifyToken = createOtp(
                user,
                OtpToken.OtpType.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24)
        );

        emailService.sendVerificationEmail(
                user.getEmail(), user.getFullName(), verifyToken
        );

        log.info("New user registered: {}", user.getEmail());
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            throw AppException.unauthorized("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (!user.getIsVerified()) {
            throw AppException.forbidden(
                    "Email not verified. Please check your inbox and verify your email before logging in."
            );
        }

        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResult(user);
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Optional<RefreshToken> validToken =
                refreshTokenRepository.findValidToken(refreshToken);

        if (validToken.isPresent()) {
            RefreshToken token = validToken.get();

            token.revoke();
            refreshTokenRepository.save(token);

            User user = token.getUser();
            String newAccessToken = jwtService.generateToken(
                    user.getId(), user.getEmail()
            );
            String newRefreshToken = createRefreshToken(user);

            log.info("Token refreshed for user: {}", user.getEmail());

            return TokenResponse.of(
                    newAccessToken, newRefreshToken, jwtService.getExpirationTime()
            );
        }

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(revokedToken -> {
                    UUID userId = revokedToken.getUser().getId();
                    refreshTokenRepository.revokeAllByUserId(userId);
                    log.warn(
                            "Refresh token reuse detected for user: {}. " +
                                    "All sessions have been revoked.",
                            revokedToken.getUser().getEmail()
                    );
                });

        throw AppException.unauthorized("Invalid or expired refresh token");
    }

    @Override
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        refreshTokenRepository.findValidToken(refreshToken)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                    log.info("User logged out: {}", token.getUser().getEmail());
                });

        blacklistAccessToken(accessToken);
    }

    @Override
    @Transactional
    public void logoutAll(String refreshToken, String accessToken) {
        refreshTokenRepository.findValidToken(refreshToken)
                .ifPresent(token -> {
                    refreshTokenRepository.revokeAllByUserId(
                            token.getUser().getId()
                    );
                    log.info("All devices logged out: {}",
                            token.getUser().getEmail());
                });

        blacklistAccessToken(accessToken);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        OtpToken otp = otpTokenRepository
                .findValidToken(token, OtpToken.OtpType.EMAIL_VERIFICATION)
                .orElseThrow(() ->
                        AppException.badRequest("Invalid or expired verification token")
                );

        if (otp.isInvalid()) {
            throw AppException.badRequest("Token has been used or expired");
        }

        User user = otp.getUser();
        user.setIsVerified(true);

        if (user.getPendingEmail() != null) {
            user.setEmail(user.getPendingEmail());
            user.setPendingEmail(null);
        }

        userRepository.save(user);

        otp.markAsUsed();
        otpTokenRepository.save(otp);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = createOtp(
                    user,
                    OtpToken.OtpType.RESET_PASSWORD,
                    LocalDateTime.now().plusMinutes(15)
            );

            emailService.sendResetPasswordEmail(
                    user.getEmail(), user.getFullName(), token
            );

            log.info("Reset password email sent to: {}", email);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpToken otp = otpTokenRepository
                .findValidToken(request.token(), OtpToken.OtpType.RESET_PASSWORD)
                .orElseThrow(() ->
                        AppException.badRequest("Invalid or expired reset token")
                );

        if (otp.isInvalid()) {
            throw AppException.badRequest("Token has been used or expired");
        }

        User user = otp.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        otp.markAsUsed();
        otpTokenRepository.save(otp);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password reset for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(
            String email,
            ChangePasswordRequest request,
            String accessToken) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (!passwordEncoder.matches(
                request.currentPassword(), user.getPasswordHash())) {
            throw AppException.badRequest("Current password is incorrect");
        }

        if (passwordEncoder.matches(
                request.newPassword(), user.getPasswordHash())) {
            throw AppException.badRequest(
                    "New password must be different from current password"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        blacklistAccessToken(accessToken);

        log.info("Password changed for user: {}", email);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getIsVerified()) {
                throw AppException.badRequest("Email already verified");
            }

            otpTokenRepository.invalidateAllByUserIdAndType(
                    user.getId(),
                    OtpToken.OtpType.EMAIL_VERIFICATION
            );

            String token = createOtp(
                    user,
                    OtpToken.OtpType.EMAIL_VERIFICATION,
                    LocalDateTime.now().plusHours(24)
            );

            emailService.sendVerificationEmail(
                    user.getEmail(), user.getFullName(), token
            );

            log.info("Verification email resent to: {}", email);
        });
    }

    private AuthResult buildAuthResult(User user) {
        String accessToken = jwtService.generateToken(
                user.getId(), user.getEmail()
        );
        String refreshToken = createRefreshToken(user);

        AuthResponse authResponse = AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                accessToken,
                jwtService.getExpirationTime()
        );

        return new AuthResult(authResponse, refreshToken);
    }

    private String createRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now()
                        .plusSeconds(refreshExpiration / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    private String createOtp(
            User user,
            OtpToken.OtpType type,
            LocalDateTime expiresAt) {

        otpTokenRepository.invalidateAllByUserIdAndType(user.getId(), type);

        String token = UUID.randomUUID().toString();
        OtpToken otp = OtpToken.builder()
                .user(user)
                .token(token)
                .type(type)
                .expiresAt(expiresAt)
                .build();

        otpTokenRepository.save(otp);
        return token;
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken.isBlank()) {
            return;
        }

        if (!jwtService.isTokenValid(accessToken)) {
            return;
        }

        String jti = jwtService.extractJti(accessToken);
        long ttl = jwtService.getRemainingTtlMillis(accessToken);
        blacklistService.blacklist(jti, ttl);

        log.debug("Access token added to blacklist. jti={}", jti);
    }
}