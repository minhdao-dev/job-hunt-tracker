package com.jobhunt.tracker.module.auth.service;

import com.jobhunt.tracker.module.auth.dto.AuthResponse;
import com.jobhunt.tracker.module.auth.dto.LoginRequest;
import com.jobhunt.tracker.module.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}