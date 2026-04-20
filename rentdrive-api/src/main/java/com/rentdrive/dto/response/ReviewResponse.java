package com.rentdrive.dto.response;

import com.rentdrive.enums.ReviewType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID bookingId,
        ReviewType type,
        Byte rating,
        String comment,
        AuthorInfo author,
        LocalDateTime createdAt
) {
    public record AuthorInfo(UUID id, String fullName) {}
}
