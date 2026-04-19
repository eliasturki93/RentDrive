package com.rentdrive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PUT /api/v1/users/{id}/password */
public record ChangePasswordRequest(
    @NotBlank(message = "L'ancien mot de passe est obligatoire") String currentPassword,
    @NotBlank @Size(min = 8, max = 72)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).+$",
             message = "Le nouveau mot de passe doit contenir au moins 1 majuscule et 1 chiffre")
    String newPassword,
    @NotBlank(message = "La confirmation du mot de passe est obligatoire") String confirmPassword
) {}
