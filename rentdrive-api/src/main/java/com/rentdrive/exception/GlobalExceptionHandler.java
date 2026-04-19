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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ErrorResponse(
        boolean success,
        String  errorCode,
        String  message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
    ) {}

    /** Exceptions métier Karrent */
    @ExceptionHandler(KarrentException.class)
    ResponseEntity<ErrorResponse> handleKarrent(KarrentException ex) {
        Map<String, String> fields = (ex instanceof ValidationException ve)
            ? ve.getFieldErrors() : null;
        return ResponseEntity.status(ex.getStatus())
            .body(new ErrorResponse(false, ex.getErrorCode(),
                                    ex.getMessage(), fields, LocalDateTime.now()));
    }

    /** Violations Bean Validation (@Valid sur les requêtes) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(false, "VALIDATION_ERROR",
                                    "Erreurs de validation", errors, LocalDateTime.now()));
    }

    /** Catch-all pour les exceptions non prévues */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(false, "INTERNAL_ERROR",
                                    "Erreur interne du serveur", null, LocalDateTime.now()));
    }
}
