package com.rentdrive.dto.request;

import com.rentdrive.enums.ReviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReviewRequest(
        @NotNull UUID bookingId,
        @NotNull @Min(1) @Max(5) Byte rating,
        String comment,
        @NotNull ReviewType type
) {}
