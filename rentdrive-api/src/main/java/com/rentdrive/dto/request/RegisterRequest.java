// =============================================================================
//  DTOs REQUEST
//  Fichier unique pour la lisibilité — séparer en production.
// =============================================================================

// ── RegisterRequest ───────────────────────────────────────────────────────────
package com.rentdrive.dto.request;

// POST /api/v1/auth/register
public record RegisterRequest(

    @jakarta.validation.constraints.NotBlank(message = "L'email est obligatoire")
    @jakarta.validation.constraints.Email(message = "Format d'email invalide")
    @jakarta.validation.constraints.Size(max = 255)
    String email,

    @jakarta.validation.constraints.NotBlank(message = "Le téléphone est obligatoire")
    @jakarta.validation.constraints.Pattern(
        regexp = "^\\+213[5-7]\\d{8}$",
        message = "Format téléphone algérien invalide (ex: +213661234567)")
    String phone,

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min = 8, max = 72)
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9]).+$",
        message = "Le mot de passe doit contenir au moins 1 majuscule et 1 chiffre")
    String password,

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min = 2, max = 100)
    String firstName,

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min = 2, max = 100)
    String lastName,

    String wilaya,

    /** Rôle demandé — défaut LOCATAIRE si null. */
    com.rentdrive.enums.RoleName requestedRole
) {}
