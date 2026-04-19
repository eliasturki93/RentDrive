package com.rentdrive.exception;

import org.springframework.http.HttpStatus;

// ── 403 ───────────────────────────────────────────────────────────────────────
public class ForbiddenException extends KarrentException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}
