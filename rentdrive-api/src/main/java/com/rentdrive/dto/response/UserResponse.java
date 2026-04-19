package com.rentdrive.dto.response;

import com.rentdrive.enums.RoleName;
import com.rentdrive.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID; /** Réponse complète — endpoint /me et admin /users/{id}. NE contient JAMAIS le passwordHash. */
public record UserResponse(
    UUID            id,
    String          email,
    String          phone,
    boolean         emailVerified,
    boolean         phoneVerified,
    UserStatus      status,
    Set<RoleName>   roles,
    ProfileResponse profile,
    LocalDateTime   createdAt
) {}
