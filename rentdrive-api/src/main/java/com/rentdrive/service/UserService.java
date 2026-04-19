package com.rentdrive.service;

import com.rentdrive.enums.RoleName;
import com.rentdrive.enums.UserStatus;
import com.rentdrive.dto.request.ChangePasswordRequest;
import com.rentdrive.dto.request.RegisterRequest;
import com.rentdrive.dto.request.UpdateProfileRequest;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.UserResponse;
import com.rentdrive.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Contrat du service utilisateur.
 *
 * Interface explicite (pattern Service/ServiceImpl) pour deux raisons :
 *  1. Testabilité : on peut mocker l'interface sans Mockito.spy()
 *  2. Proxy Spring AOP : @Transactional fonctionne correctement sur les interfaces
 *
 * Toutes les méthodes lancent des exceptions de com.rentdrive.exception
 * documentées dans l'implémentation.
 */
public interface UserService {

    // ── Inscription & profil ──────────────────────────────────────────────────

    /** Crée un nouveau compte utilisateur. Lève ConflictException si email/phone déjà pris. */
    UserResponse register(RegisterRequest request);

    /** Retourne l'utilisateur courant (endpoint /me). */
    UserResponse getCurrentUser(UUID userId);

    /** Retourne un utilisateur par son ID (admin ou propriétaire du compte). */
    UserResponse getById(UUID id);

    /** Met à jour les données de profil (nom, wilaya, avatar…). */
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /** Changement de mot de passe avec vérification de l'ancien. */
    void changePassword(UUID userId, ChangePasswordRequest request);

    // ── Vérifications ────────────────────────────────────────────────────────

    /** Marque l'email comme vérifié (appelé depuis le lien d'activation). */
    void verifyEmail(UUID userId);

    /** Marque le téléphone comme vérifié (appelé après validation OTP). */
    void verifyPhone(UUID userId);

    // ── Administration ────────────────────────────────────────────────────────

    /** Liste paginée avec recherche full-text (backoffice admin). */
    PageResponse<UserSummaryResponse> searchUsers(String query, Pageable pageable);

    /** Liste des utilisateurs ayant un rôle donné. */
    List<UserSummaryResponse> getUsersByRole(RoleName role);

    /** Modifie le statut d'un utilisateur (ACTIVE / SUSPENDED / BANNED). */
    UserResponse updateStatus(UUID userId, UserStatus status, String reason);

    /** Assigne un rôle supplémentaire à un utilisateur. */
    UserResponse assignRole(UUID userId, RoleName role);

    /** Révoque un rôle d'un utilisateur. */
    UserResponse revokeRole(UUID userId, RoleName role);

    /** Supprime définitivement un compte (RGPD — soft delete recommandé). */
    void deleteAccount(UUID userId);
}
