package com.rentdrive.controller;

import com.rentdrive.enums.RoleName;
import com.rentdrive.enums.UserStatus;
import com.rentdrive.dto.request.ChangePasswordRequest;
import com.rentdrive.dto.request.RegisterRequest;
import com.rentdrive.dto.request.UpdateProfileRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.UserResponse;
import com.rentdrive.dto.response.UserSummaryResponse;
import com.rentdrive.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour la ressource User.
 *
 * Routes :
 *  POST   /api/v1/auth/register              → inscription publique
 *  GET    /api/v1/users/me                   → profil courant
 *  GET    /api/v1/users/{id}                 → profil par ID (admin)
 *  PATCH  /api/v1/users/me/profile           → mise à jour profil
 *  PUT    /api/v1/users/me/password          → changement mot de passe
 *  POST   /api/v1/users/me/verify-email      → vérification email
 *  POST   /api/v1/users/me/verify-phone      → vérification téléphone
 *  GET    /api/v1/admin/users                → liste paginée (admin)
 *  PATCH  /api/v1/admin/users/{id}/status    → mise à jour statut (admin)
 *  POST   /api/v1/admin/users/{id}/roles     → assignation rôle (admin)
 *  DELETE /api/v1/admin/users/{id}/roles/{r} → révocation rôle (admin)
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // =========================================================================
    // INSCRIPTION (public — pas d'auth requise)
    // =========================================================================

    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Compte créé avec succès. Vérifiez votre email.", user));
    }

    // =========================================================================
    // PROFIL COURANT — endpoints /me
    // =========================================================================

    /**
     * Retourne le profil de l'utilisateur connecté.
     * @AuthenticationPrincipal injecté par Spring Security après validation JWT.
     */
    @GetMapping("/api/v1/users/me")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(userService.getCurrentUser(userId)));
    }

    @PatchMapping("/api/v1/users/me/profile")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Profil mis à jour.", userService.updateProfile(userId, request)));
    }

    @PutMapping("/api/v1/users/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        UUID userId = UUID.fromString(principal.getUsername());
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe mis à jour avec succès.", null));
    }

    @PostMapping("/api/v1/users/me/verify-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyMyEmail(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        userService.verifyEmail(userId);
        return ResponseEntity.ok(ApiResponse.ok("Email vérifié.", null));
    }

    @PostMapping("/api/v1/users/me/verify-phone")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyMyPhone(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        userService.verifyPhone(userId);
        return ResponseEntity.ok(ApiResponse.ok("Téléphone vérifié.", null));
    }

    @DeleteMapping("/api/v1/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.ok("Compte supprimé.", null));
    }

    // =========================================================================
    // ADMINISTRATION — ROLE_ADMIN uniquement
    // =========================================================================

    /**
     * Liste paginée des utilisateurs avec recherche optionnelle.
     * Exemple : GET /api/v1/admin/users?q=lyes&page=0&size=20&sort=createdAt,desc
     */
    @GetMapping("/api/v1/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> listUsers(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort.Direction dir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageResponse<UserSummaryResponse> result = userService.searchUsers(
                q, PageRequest.of(page, size, Sort.by(dir, sortParts[0])));

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/api/v1/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @GetMapping("/api/v1/admin/users/by-role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getUsersByRole(
            @PathVariable RoleName role) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUsersByRole(role)));
    }

    /**
     * Mise à jour du statut : ACTIVE / SUSPENDED / BANNED.
     * Body optionnel : { "reason": "Fraude détectée" }
     */
    @PatchMapping("/api/v1/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam UserStatus status,
            @RequestParam(required = false) String reason) {

        return ResponseEntity.ok(
                ApiResponse.ok("Statut mis à jour.", userService.updateStatus(id, status, reason)));
    }

    @PostMapping("/api/v1/admin/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable UUID id,
            @RequestParam RoleName role) {

        return ResponseEntity.ok(
                ApiResponse.ok("Rôle " + role + " assigné.", userService.assignRole(id, role)));
    }

    @DeleteMapping("/api/v1/admin/users/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> revokeRole(
            @PathVariable UUID id,
            @PathVariable RoleName role) {

        return ResponseEntity.ok(
                ApiResponse.ok("Rôle " + role + " révoqué.", userService.revokeRole(id, role)));
    }
}
