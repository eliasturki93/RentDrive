package com.rentdrive.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

// =============================================================================
//  RESPONSE DTOs — tous dans ce fichier pour la lisibilité
// =============================================================================

/** Enveloppe API uniforme. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, LocalDateTime timestamp) {
    static <T> ApiResponse<T> ok(T data)                   { return new ApiResponse<>(true, null, data, LocalDateTime.now()); }
    static <T> ApiResponse<T> ok(String msg, T data)       { return new ApiResponse<>(true, msg,  data, LocalDateTime.now()); }
    static <T> ApiResponse<T> created(String msg, T data)  { return new ApiResponse<>(true, msg,  data, LocalDateTime.now()); }
    static <T> ApiResponse<T> error(String msg)            { return new ApiResponse<>(false, msg, null, LocalDateTime.now()); }
}

