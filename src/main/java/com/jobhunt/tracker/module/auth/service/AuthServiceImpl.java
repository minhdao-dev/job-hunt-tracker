package com.jobhunt.tracker.module.auth.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.auth.dto.AuthResponse;
import com.jobhunt.tracker.module.auth.dto.LoginRequest;
import com.jobhunt.tracker.module.auth.dto.RegisterRequest;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

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

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Generate JWT
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                token,
                jwtService.getExpirationTime()
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {

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
                .orElseThrow(() ->
                        AppException.notFound("User not found")
                );

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                token,
                jwtService.getExpirationTime()
        );
    }
}