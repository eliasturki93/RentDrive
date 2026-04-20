package com.rentdrive.dto.response;

import com.rentdrive.enums.StoreStatus;
import com.rentdrive.enums.StoreType;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreSummaryResponse(
        UUID        id,
        String      name,
        StoreType   type,
        StoreStatus status,
        String      logoUrl,
        String      wilaya,
        String      city,
        BigDecimal  rating,
        Integer     reviewCount
) {}
