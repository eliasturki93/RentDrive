package com.rentdrive.dto.response;

import com.rentdrive.enums.StoreStatus;
import com.rentdrive.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StoreResponse(
        UUID          id,
        String        name,
        StoreType     type,
        StoreStatus   status,
        String        description,
        String        logoUrl,
        String        phone,
        String        address,
        String        city,
        String        wilaya,
        BigDecimal    rating,
        Integer       reviewCount,
        OwnerInfo     owner,
        LocalDateTime createdAt
) {
    public record OwnerInfo(UUID id, String fullName, String avatarUrl) {}
}
