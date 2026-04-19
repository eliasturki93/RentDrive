package com.rentdrive.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// =============================================================================
//  HIÉRARCHIE D'EXCEPTIONS — Toutes dans un seul fichier pour la lisibilité.
//  En production : séparer en fichiers individuels.
// =============================================================================

// ── 404 ───────────────────────────────────────────────────────────────────────
public class ResourceNotFoundException extends KarrentException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " introuvable : " + id,
              HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
