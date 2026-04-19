package com.rentdrive.service;

import com.rentdrive.dto.request.LoginRequest;
import com.rentdrive.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String accessToken);
}
