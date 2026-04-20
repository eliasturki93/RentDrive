package com.rentdrive.dto.response;

import com.rentdrive.enums.DocumentType;
import com.rentdrive.enums.VerifStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        DocumentType type,
        String fileUrl,
        VerifStatus status,
        String rejectionReason,
        LocalDate expiryDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
