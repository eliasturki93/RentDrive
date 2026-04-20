package com.rentdrive.dto.response;

import com.rentdrive.enums.FuelType;
import com.rentdrive.enums.Transmission;
import com.rentdrive.enums.VehicleCategory;
import com.rentdrive.enums.VehicleStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleSummaryResponse(
        UUID            id,
        String          brand,
        String          model,
        short           year,
        VehicleCategory category,
        Transmission    transmission,
        FuelType        fuelType,
        BigDecimal      pricePerDay,
        VehicleStatus   status,
        String          primaryPhotoUrl,
        String          wilaya,
        UUID            storeId,
        String          storeName
) {}
