package com.rentdrive.dto.request;

import com.rentdrive.enums.FuelType;
import com.rentdrive.enums.Transmission;
import com.rentdrive.enums.VehicleCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateVehicleRequest(

        @Size(max = 100)
        String brand,

        @Size(max = 100)
        String model,

        @Min(1990) @Max(2030)
        Short year,

        VehicleCategory category,
        Transmission    transmission,
        FuelType        fuelType,

        @Min(1) @Max(9)
        Byte seats,

        @PositiveOrZero
        Integer mileage,

        @DecimalMin("0.01")
        BigDecimal pricePerDay,

        @DecimalMin("0.01")
        BigDecimal pricePerWeek,

        @DecimalMin("0.0")
        BigDecimal depositAmount,

        Map<String, Object> features,

        @Size(max = 2000)
        String description,

        @Size(max = 100)
        String wilaya
) {}
