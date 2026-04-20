package com.rentdrive.dto.request;

import com.rentdrive.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UploadDocumentRequest(
        @NotNull DocumentType type,
        @NotBlank String fileUrl,
        LocalDate expiryDate
) {}
