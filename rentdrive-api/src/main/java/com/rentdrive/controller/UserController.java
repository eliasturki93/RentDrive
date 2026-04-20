package com.rentdrive.controller;

import com.rentdrive.enums.RoleName;
import com.rentdrive.enums.UserStatus;
import com.rentdrive.dto.request.ChangePasswordRequest;
import com.rentdrive.dto.request.UpdateProfileRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.UserResponse;
import com.rentdrive.dto.response.UserSummaryResponse;
import com.rentdrive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Utilisateurs", description = "Gestion du profil personnel et administration des comptes utilisateurs")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Profil personnel ──────────────────────────────────────────────────────

    @Operation(
        summary = "Mon profil",
        description = "Retourne les informations complètes du compte connecté : " +
                      "email, téléphone, statut, rôles, et données de profil (nom, prénom, avatar, adresse)."
    )
    @GetMapping("/api/v1/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(userService.getCurrentUser(userId)));
    }

    @Operation(
        summary = "Mettre à jour mon profil",
        description = "Modifie les données personnelles de l'utilisateur connecté : " +
                      "prénom, nom, date de naissance, adresse, biographie, avatar. " +
                      "Seuls les champs envoyés sont modifiés (PATCH sémantique)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profil mis à jour"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PatchMapping("/api/v1/users/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Profil mis à jour.", userService.updateProfile(userId, request)));
    }

    @Operation(
        summary = "Changer mon mot de passe",
        description = "Vérifie l'ancien mot de passe puis applique le nouveau. " +
                      "Le nouveau mot de passe doit faire au moins 8 caractères."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mot de passe changé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ancien mot de passe incorrect ou nouveau invalide")
    })
    @PutMapping("/api/v1/users/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        UUID userId = UUID.fromString(principal.getUsername());
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe mis à jour avec succès.", null));
    }

    @Operation(
        summary = "Vérifier mon email",
        description = "Marque l'email de l'utilisateur connecté comme vérifié. " +
                      "En production, cette action sera déclenchée par un lien envoyé par email."
    )
    @PostMapping("/api/v1/users/me/verify-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyMyEmail(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        userService.verifyEmail(userId);
        return ResponseEntity.ok(ApiResponse.ok("Email vérifié.", null));
    }

    @Operation(
        summary = "Vérifier mon téléphone",
        description = "Marque le téléphone de l'utilisateur connecté comme vérifié. " +
                      "En production, cette action sera déclenchée par un code OTP SMS."
    )
    @PostMapping("/api/v1/users/me/verify-phone")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyMyPhone(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        userService.verifyPhone(userId);
        return ResponseEntity.ok(ApiResponse.ok("Téléphone vérifié.", null));
    }

    @Operation(
        summary = "Supprimer mon compte",
        description = "Supprime définitivement le compte de l'utilisateur connecté. " +
                      "Impossible si des réservations sont en cours (PENDING, CONFIRMED ou IN_PROGRESS). " +
                      "Cette action est irréversible."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Compte supprimé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Réservations actives en cours, suppression impossible")
    })
    @DeleteMapping("/api/v1/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.ok("Compte supprimé.", null));
    }

    // ── Administration ────────────────────────────────────────────────────────

    @Operation(
        summary = "[Admin] Lister les utilisateurs",
        description = "Retourne la liste paginée de tous les utilisateurs avec recherche full-text optionnelle. " +
                      "La recherche porte sur le prénom, le nom et l'email. " +
                      "Exemple : GET /api/v1/admin/users?q=lyes&page=0&size=20&sort=createdAt,desc"
    )
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

    @Operation(
        summary = "[Admin] Détail d'un utilisateur",
        description = "Retourne le profil complet d'un utilisateur par son ID, " +
                      "incluant ses rôles, son store et ses documents KYC."
    )
    @GetMapping("/api/v1/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @Operation(
        summary = "[Admin] Lister les utilisateurs par rôle",
        description = "Retourne tous les utilisateurs actifs possédant un rôle précis. " +
                      "Rôles disponibles : LOCATAIRE, BAILLEUR, AGENCE, ADMIN."
    )
    @GetMapping("/api/v1/admin/users/by-role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getUsersByRole(
            @PathVariable RoleName role) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUsersByRole(role)));
    }

    @Operation(
        summary = "[Admin] Changer le statut d'un utilisateur",
        description = "Modifie le statut d'un compte : ACTIVE (réactivation), SUSPENDED (suspension temporaire) " +
                      "ou BANNED (bannissement définitif). Un utilisateur SUSPENDED ou BANNED ne peut plus se connecter."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut mis à jour"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PatchMapping("/api/v1/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam UserStatus status,
            @RequestParam(required = false) String reason) {

        return ResponseEntity.ok(
                ApiResponse.ok("Statut mis à jour.", userService.updateStatus(id, status, reason)));
    }

    @Operation(
        summary = "[Admin] Assigner un rôle",
        description = "Ajoute un rôle supplémentaire à un utilisateur. " +
                      "Un utilisateur peut cumuler plusieurs rôles (ex : LOCATAIRE + BAILLEUR). " +
                      "Rôles disponibles : LOCATAIRE, BAILLEUR, AGENCE, ADMIN."
    )
    @PostMapping("/api/v1/admin/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable UUID id,
            @RequestParam RoleName role) {

        return ResponseEntity.ok(
                ApiResponse.ok("Rôle " + role + " assigné.", userService.assignRole(id, role)));
    }

    @Operation(
        summary = "[Admin] Révoquer un rôle",
        description = "Retire un rôle spécifique d'un utilisateur. " +
                      "Le rôle est passé dans l'URL. Ex : DELETE /api/v1/admin/users/{id}/roles/BAILLEUR"
    )
    @DeleteMapping("/api/v1/admin/users/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> revokeRole(
            @PathVariable UUID id,
            @PathVariable RoleName role) {

        return ResponseEntity.ok(
                ApiResponse.ok("Rôle " + role + " révoqué.", userService.revokeRole(id, role)));
    }
}
