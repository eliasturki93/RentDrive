package com.rentdrive.dto.response;

import com.rentdrive.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID          id,
        BookingStatus status,
        LocalDate     startDate,
        LocalDate     endDate,
        short         totalDays,
        BigDecimal    pricePerDay,
        BigDecimal    subtotal,
        BigDecimal    commission,
        BigDecimal    depositAmount,
        BigDecimal    totalAmount,
        String        cancellationReason,
        LocalDateTime confirmedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        VehicleInfo   vehicle,
        RenterInfo    renter,
        StoreInfo     store,
        PaymentInfo   payment
) {
    public record VehicleInfo(UUID id, String brand, String model, short year, String primaryPhotoUrl) {}
    public record RenterInfo(UUID id, String fullName, String phone) {}
    public record StoreInfo(UUID id, String name, String phone) {}
    public record PaymentInfo(UUID id, PaymentMethod method, PaymentStatus status,
                              DepositStatus depositStatus, String transactionRef) {}
}
