package com.rentdrive.dto.response;

import com.rentdrive.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingSummaryResponse(
        UUID          id,
        BookingStatus status,
        LocalDate     startDate,
        LocalDate     endDate,
        BigDecimal    totalAmount,
        LocalDateTime createdAt,
        String        vehicleLabel,
        String        primaryPhotoUrl,
        String        renterName,
        String        storeName
) {}
