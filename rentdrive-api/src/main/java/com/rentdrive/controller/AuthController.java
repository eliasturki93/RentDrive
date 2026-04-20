package com.rentdrive.controller;

import com.rentdrive.dto.request.LoginRequest;
import com.rentdrive.dto.request.RegisterRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.AuthResponse;
import com.rentdrive.dto.response.UserResponse;
import com.rentdrive.service.AuthService;
import com.rentdrive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentification", description = "Inscription, connexion, renouvellement de token et déconnexion")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(
        summary = "Créer un compte",
        description = "Inscrit un nouvel utilisateur (locataire, bailleur ou agence). " +
                      "Retourne le profil créé. Un email de vérification est envoyé (simulation)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email ou téléphone déjà utilisé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides (validation échouée)")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Compte créé avec succès. Vérifiez votre email.", user));
    }

    @Operation(
        summary = "Se connecter",
        description = "Authentifie l'utilisateur avec email + mot de passe. " +
                      "Retourne un access token (15 min) et un refresh token (7 jours). " +
                      "Inclure l'access token dans le header : Authorization: Bearer {token}"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connexion réussie, tokens retournés"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Compte suspendu ou banni")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Connexion réussie.", auth));
    }

    @Operation(
        summary = "Renouveler l'access token",
        description = "Échange un refresh token valide contre une nouvelle paire access/refresh token. " +
                      "L'ancien refresh token est invalidé (rotation). " +
                      "Passer le refresh token dans le header : X-Refresh-Token: {token}"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Nouveaux tokens retournés"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token expiré ou invalide")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        AuthResponse auth = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok("Token renouvelé.", auth));
    }

    @Operation(
        summary = "Se déconnecter",
        description = "Invalide l'access token courant en l'ajoutant à la blacklist Redis. " +
                      "Après déconnexion, le token ne sera plus accepté même s'il n'est pas expiré. " +
                      "Nécessite : Authorization: Bearer {token}"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
    })
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.ok("Déconnexion réussie.", null));
    }
}
