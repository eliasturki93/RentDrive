package com.rentdrive.exception;

import org.springframework.http.HttpStatus;

// ── Exception de base ─────────────────────────────────────────────────────────
public class KarrentException extends RuntimeException {
    private final HttpStatus status;
    private final String     errorCode;

    public KarrentException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }
    public HttpStatus getStatus()    { return status; }
    public String     getErrorCode() { return errorCode; }
}
