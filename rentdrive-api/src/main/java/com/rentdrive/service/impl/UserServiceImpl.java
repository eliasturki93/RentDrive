package com.rentdrive.service.impl;

import com.rentdrive.entity.Profile;
import com.rentdrive.entity.Role;
import com.rentdrive.entity.User;
import com.rentdrive.enums.RoleName;
import com.rentdrive.enums.UserStatus;
import com.rentdrive.dto.request.ChangePasswordRequest;
import com.rentdrive.dto.request.RegisterRequest;
import com.rentdrive.dto.request.UpdateProfileRequest;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.UserResponse;
import com.rentdrive.dto.response.UserSummaryResponse;
import com.rentdrive.exception.ConflictException;
import com.rentdrive.exception.ForbiddenException;
import com.rentdrive.exception.ResourceNotFoundException;
import com.rentdrive.exception.ValidationException;
import com.rentdrive.mapper.UserMapper;
import com.rentdrive.repository.RoleRepository;
import com.rentdrive.repository.UserRepository;
import com.rentdrive.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation du service utilisateur.
 *
 * Toutes les méthodes qui modifient des données sont annotées @Transactional.
 * Les lectures seules sont annotées @Transactional(readOnly = true) :
 *  - Hibernate désactive le dirty checking → ~30% plus rapide sur de grandes collections
 *  - Indicateur pour le pool de connexions (potentiel routage sur un replica read-only)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper      userMapper;

    // =========================================================================
    // INSCRIPTION
    // =========================================================================

    /**
     * Crée un nouveau compte utilisateur.
     *
     * Flux :
     *  1. Vérification unicité email + téléphone (ConflictException si doublon)
     *  2. Résolution du rôle demandé (défaut : LOCATAIRE)
     *  3. Hash du mot de passe via BCrypt
     *  4. Création User + Profile + liaison rôle dans une transaction atomique
     *  5. TODO : publier un événement "UserRegistered" sur RabbitMQ
     *             → Notification Service envoie l'email de bienvenue
     */
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Inscription : {}", request.email());

        // Vérifications unicité
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(
                "Un compte existe déjà avec l'email : " + request.email());
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new ConflictException(
                "Un compte existe déjà avec ce numéro de téléphone.");
        }

        // Résolution du rôle demandé
        RoleName roleName = (request.requestedRole() != null)
                ? request.requestedRole()
                : RoleName.LOCATAIRE;

        // Bloquer la création directe d'un compte ADMIN via l'API publique
        if (roleName == RoleName.ADMIN) {
            throw new ForbiddenException(
                "La création d'un compte ADMIN est réservée à l'administration.");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));

        // Construction de l'entité User
        User user = new User();
        user.setEmail(request.email().toLowerCase().trim());
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(role);

        // Construction du Profile — lié au User
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFirstName(capitalize(request.firstName()));
        profile.setLastName(capitalize(request.lastName()).toUpperCase());
        profile.setWilaya(request.wilaya());
        user.setProfile(profile);

        User saved = userRepository.save(user);
        log.info("Compte créé : {} ({})", saved.getId(), saved.getEmail());

        // TODO: eventPublisher.publish(new UserRegisteredEvent(saved.getId(), saved.getEmail()));

        return userMapper.toResponse(saved);
    }

    // =========================================================================
    // LECTURE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        // Chargement explicite des rôles dans la même transaction
        user.getRoles().size(); // force l'init du Set<Role> LAZY
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        /*
         * Utilise findByIdWithAllRelations uniquement sur les endpoints admin.
         * Pour les endpoints publics : findByIdWithProfile suffit.
         */
        User user = userRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponse(user);
    }

    // =========================================================================
    // MISE À JOUR PROFIL
    // =========================================================================

    /**
     * Mise à jour partielle (PATCH) du profil.
     * Seuls les champs non-null dans la requête sont mis à jour.
     * Cette approche "partial update" évite d'écraser des données avec null.
     */
    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.debug("Mise à jour profil user {}", userId);

        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Profile profile = user.getProfile();

        // Mise à jour partielle — uniquement les champs fournis
        if (StringUtils.hasText(request.firstName())) {
            profile.setFirstName(capitalize(request.firstName()));
        }
        if (StringUtils.hasText(request.lastName())) {
            profile.setLastName(capitalize(request.lastName()).toUpperCase());
        }
        if (request.dateOfBirth() != null) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }
        if (StringUtils.hasText(request.wilaya())) {
            profile.setWilaya(request.wilaya());
        }
        if (StringUtils.hasText(request.city())) {
            profile.setCity(request.city());
        }
        if (StringUtils.hasText(request.address())) {
            profile.setAddress(request.address());
        }
        if (StringUtils.hasText(request.avatarUrl())) {
            profile.setAvatarUrl(request.avatarUrl());
        }

        /*
         * Pas besoin d'appeler save() explicitement :
         * Profile est une entité managed dans la transaction en cours.
         * Hibernate détecte les changements via dirty checking et génère
         * un UPDATE automatique au commit de la transaction.
         */

        log.info("Profil mis à jour : {}", userId);
        return userMapper.toResponse(user);
    }

    // =========================================================================
    // CHANGEMENT DE MOT DE PASSE
    // =========================================================================

    /**
     * Changement de mot de passe avec vérification de l'actuel.
     *
     * Points de sécurité :
     *  - passwordEncoder.matches() est constant-time (pas d'attaque timing)
     *  - Utilise @Modifying UPDATE direct → ne passe pas par le cache JPA
     *  - Ne retourne pas le User complet (réponse vide = bonne pratique)
     */
    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        // Validation métier : confirmation = nouveau mot de passe
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ValidationException(
                "Les mots de passe ne correspondent pas.",
                Map.of("confirmPassword", "Doit être identique au nouveau mot de passe")
            );
        }

        // Vérification de l'ancien mot de passe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException(
                "Mot de passe actuel incorrect.",
                Map.of("currentPassword", "Mot de passe incorrect")
            );
        }

        // Vérification : nouveau ≠ ancien
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ValidationException(
                "Le nouveau mot de passe doit être différent de l'actuel.",
                Map.of("newPassword", "Identique au mot de passe actuel")
            );
        }

        String newHash = passwordEncoder.encode(request.newPassword());

        // UPDATE SQL direct via @Modifying — plus rapide qu'un save()
        int updated = userRepository.updatePasswordHash(userId, newHash);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", userId);
        }

        log.info("Mot de passe mis à jour pour user {}", userId);
        // TODO: eventPublisher.publish(new PasswordChangedEvent(userId));
        //       → Notification Service envoie un email d'alerte sécurité
    }

    // =========================================================================
    // VÉRIFICATIONS EMAIL / TÉLÉPHONE
    // =========================================================================

    @Override
    @Transactional
    public void verifyEmail(UUID userId) {
        int updated = userRepository.markEmailVerified(userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", userId);
        }
        log.info("Email vérifié pour user {}", userId);
    }

    @Override
    @Transactional
    public void verifyPhone(UUID userId) {
        int updated = userRepository.markPhoneVerified(userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", userId);
        }
        log.info("Téléphone vérifié pour user {}", userId);
    }

    // =========================================================================
    // ADMINISTRATION
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(String query, Pageable pageable) {
        /*
         * Si query est vide : retourne tous les users paginés via findAll().
         * Si query fourni  : filtre sur prénom/nom/email.
         *
         * Note : Pour une vraie recherche full-text en production,
         * utiliser Elasticsearch (Search Service) — MySQL LIKE ne scale pas.
         */
        Page<User> page;
        if (!StringUtils.hasText(query)) {
            page = userRepository.findAll(pageable);
        } else {
            page = userRepository.searchUsers(query.trim(), pageable);
        }

        Page<UserSummaryResponse> mapped = page.map(userMapper::toSummary);
        return PageResponse.of(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUsersByRole(RoleName role) {
        return userRepository.findActiveUsersByRole(role)
                .stream()
                .map(userMapper::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * Mise à jour du statut utilisateur (admin only).
     *
     * Règles métier :
     *  - Un ADMIN ne peut pas se bannir lui-même
     *  - Le passage de BANNED à ACTIVE nécessite une vérification KYC complète
     *    (ici simplifié — ajouter la vérification si besoin)
     */
    @Override
    @Transactional
    public UserResponse updateStatus(UUID userId, UserStatus status, String reason) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ADMIN);

        if (isAdmin && status == UserStatus.BANNED) {
            throw new ForbiddenException("Impossible de bannir un compte administrateur.");
        }

        user.setStatus(status);

        log.info("Statut user {} → {} (raison: {})", userId, status, reason);
        // TODO: eventPublisher.publish(new UserStatusChangedEvent(userId, status, reason));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse assignRole(UUID userId, RoleName roleName) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Charger les rôles actuels dans la transaction
        user.getRoles().size();

        boolean alreadyHasRole = user.getRoles().stream()
                .anyMatch(r -> r.getName() == roleName);

        if (alreadyHasRole) {
            throw new ConflictException(
                "L'utilisateur possède déjà le rôle " + roleName);
        }

        // Règle métier : AGENCE nécessite vérification KYC avant d'assigner le rôle
        if (roleName == RoleName.AGENCE) {
            long verifiedDocs = user.getDocuments().stream()
                    .filter(d -> d.getStatus().name().equals("VERIFIED"))
                    .count();
            if (verifiedDocs == 0) {
                throw new ValidationException(
                    "Documents KYC non vérifiés — impossible d'assigner le rôle AGENCE.",
                    Map.of("documents", "Aucun document vérifié trouvé")
                );
            }
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));

        user.getRoles().add(role);
        log.info("Rôle {} assigné à user {}", roleName, userId);

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse revokeRole(UUID userId, RoleName roleName) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.getRoles().size();

        // Garder au minimum 1 rôle (LOCATAIRE)
        if (user.getRoles().size() == 1) {
            throw new ValidationException(
                "Impossible de révoquer le dernier rôle d'un utilisateur.",
                Map.of("role", "Un utilisateur doit conserver au moins un rôle")
            );
        }

        boolean removed = user.getRoles()
                .removeIf(r -> r.getName() == roleName);

        if (!removed) {
            throw new ResourceNotFoundException(
                "L'utilisateur ne possède pas le rôle " + roleName, userId);
        }

        log.info("Rôle {} révoqué pour user {}", roleName, userId);
        return userMapper.toResponse(user);
    }

    /**
     * Suppression de compte.
     *
     * Production : toujours préférer le soft delete (is_deleted + deleted_at)
     * aux hard deletes, pour conserver l'intégrité des bookings/reviews liés.
     *
     * Ici : hard delete pour la démo. En prod, remplacer par :
     *   user.setStatus(UserStatus.DELETED);
     *   user.setEmail("deleted-" + userId + "@karrent.invalid");
     *   user.setPhone("+213000000000");
     * + purge des PII dans Profile.
     */
    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Vérification : l'utilisateur n'a pas de réservation active
        boolean hasActiveBooking = user.getBookings().stream()
                .anyMatch(b -> {
                    String s = b.getStatus().name();
                    return s.equals("PENDING")
                        || s.equals("CONFIRMED")
                        || s.equals("IN_PROGRESS");
                });

        if (hasActiveBooking) {
            throw new ValidationException(
                "Impossible de supprimer un compte avec des réservations en cours.",
                Map.of("bookings", "Réservations actives trouvées")
            );
        }

        userRepository.delete(user);
        log.info("Compte {} supprimé.", userId);
        // TODO: eventPublisher.publish(new UserDeletedEvent(userId));
    }

    // =========================================================================
    // UTILITAIRES PRIVÉS
    // =========================================================================

    /** Met en majuscule la première lettre de chaque mot. */
    private String capitalize(String input) {
        if (!StringUtils.hasText(input)) return input;
        String[] words = input.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
