package com.rentdrive.dto.request;

import com.rentdrive.enums.PaymentMethod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(

        @NotNull
        UUID vehicleId,

        @NotNull @Future
        LocalDate startDate,

        @NotNull @Future
        LocalDate endDate,

        @NotNull
        PaymentMethod paymentMethod
) {}
