package com.rentdrive.mapper;

import com.rentdrive.entity.Profile;
import com.rentdrive.entity.User;
import com.rentdrive.enums.RoleName;
import com.rentdrive.dto.response.ProfileResponse;
import com.rentdrive.dto.response.UserResponse;
import com.rentdrive.dto.response.UserSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convertit les entités JPA en DTOs de réponse.
 *
 * Pourquoi ne pas utiliser MapStruct ici ?
 * Les relations Lazy de JPA peuvent déclencher des LazyInitializationException
 * si le mapping est fait hors d'une transaction ouverte. Ce mapper manuel
 * contrôle exactement quelles relations sont accédées et quand.
 *
 * Si tu veux MapStruct : annoter les mappers @Transactional
 * ou utiliser des projections JPA directement.
 */
@Component
public class UserMapper {

    /**
     * Entité complète → DTO complet.
     * Pré-requis : user.profile et user.roles doivent être chargés (EAGER ou EntityGraph).
     */
    public UserResponse toResponse(User user) {
        Set<RoleName> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getStatus(),
                roles,
                toProfileResponse(user.getProfile()),
                user.getCreatedAt()
        );
    }

    /**
     * Version allégée pour les listes.
     * N'accède pas aux rôles — évite un JOIN supplémentaire.
     */
    public UserSummaryResponse toSummary(User user) {
        Profile profile = user.getProfile();
        String fullName = profile != null
                ? profile.getFirstName() + " " + profile.getLastName()
                : "—";
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
        String wilaya    = profile != null ? profile.getWilaya()    : null;

        return new UserSummaryResponse(
                user.getId(),
                fullName,
                avatarUrl,
                wilaya,
                user.getStatus()
        );
    }

    public ProfileResponse toProfileResponse(Profile profile) {
        if (profile == null) return null;
        return new ProfileResponse(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getFirstName() + " " + profile.getLastName(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getAvatarUrl(),
                profile.getWilaya(),
                profile.getCity(),
                profile.getAddress()
        );
    }
}
