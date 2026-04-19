package com.rentdrive.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

// ── 422 avec détail des champs en erreur ─────────────────────────────────────
public class ValidationException extends KarrentException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
