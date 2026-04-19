package com.rentdrive.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long   expiresIn,
        UserResponse user
) {}
