package com.eventflow.service;

import com.eventflow.dto.AuthResponse;
import com.eventflow.dto.LoginRequest;
import com.eventflow.dto.RegisterRequest;
import com.eventflow.dto.UserSummaryResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserSummaryResponse getCurrentUserProfile();
    UserSummaryResponse updateProfile(com.eventflow.dto.UpdateProfileRequest request);
}
