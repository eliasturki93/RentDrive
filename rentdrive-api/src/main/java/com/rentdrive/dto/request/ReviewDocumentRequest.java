package com.rentdrive.dto.request;

import com.rentdrive.enums.VerifStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewDocumentRequest(
        @NotNull VerifStatus status,
        String rejectionReason
) {}
