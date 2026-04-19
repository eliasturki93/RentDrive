package com.rentdrive.dto.response;

import com.rentdrive.enums.UserStatus;

import java.util.UUID; /** Version allégée pour les listes et les références croisées (Booking, Review…). */
public record UserSummaryResponse(
    UUID       id,
    String     fullName,
    String     avatarUrl,
    String     wilaya,
    UserStatus status
) {}
