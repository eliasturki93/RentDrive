package com.rentdrive.service;

import com.rentdrive.dto.request.CreateStoreRequest;
import com.rentdrive.dto.request.UpdateStoreRequest;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.StoreResponse;
import com.rentdrive.dto.response.StoreSummaryResponse;
import com.rentdrive.enums.StoreStatus;
import com.rentdrive.enums.StoreType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StoreService {

    StoreResponse createStore(UUID ownerId, CreateStoreRequest request);

    StoreResponse getMyStore(UUID ownerId);

    StoreResponse updateMyStore(UUID ownerId, UpdateStoreRequest request);

    StoreResponse getById(UUID storeId);

    PageResponse<StoreSummaryResponse> listApproved(String wilaya, StoreType type, Pageable pageable);

    // Admin
    StoreResponse updateStatus(UUID storeId, StoreStatus status);

    PageResponse<StoreSummaryResponse> listAll(StoreStatus status, Pageable pageable);
}
