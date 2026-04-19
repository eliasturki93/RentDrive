package com.rentdrive.exception;

import org.springframework.http.HttpStatus;

// ── 409 ───────────────────────────────────────────────────────────────────────
public class ConflictException extends KarrentException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }
}
