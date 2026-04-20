package com.rentdrive.dto.response;

import com.rentdrive.enums.FuelType;
import com.rentdrive.enums.Transmission;
import com.rentdrive.enums.VehicleCategory;
import com.rentdrive.enums.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record VehicleResponse(
        UUID                id,
        String              brand,
        String              model,
        short               year,
        VehicleCategory     category,
        Transmission        transmission,
        FuelType            fuelType,
        byte                seats,
        Integer             mileage,
        BigDecimal          pricePerDay,
        BigDecimal          pricePerWeek,
        BigDecimal          depositAmount,
        Map<String, Object> features,
        String              description,
        VehicleStatus       status,
        BigDecimal          latitude,
        BigDecimal          longitude,
        String              wilaya,
        List<PhotoInfo>     photos,
        StoreInfo           store,
        LocalDateTime       createdAt
) {
    public record PhotoInfo(UUID id, String url, boolean isPrimary, byte orderIndex) {}
    public record StoreInfo(UUID id, String name, String wilaya) {}
}
