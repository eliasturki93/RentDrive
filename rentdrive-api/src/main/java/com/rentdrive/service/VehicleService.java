package com.rentdrive.service;

import com.rentdrive.dto.request.AddPhotoRequest;
import com.rentdrive.dto.request.CreateVehicleRequest;
import com.rentdrive.dto.request.UpdateVehicleRequest;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.VehicleResponse;
import com.rentdrive.dto.response.VehicleSummaryResponse;
import com.rentdrive.enums.FuelType;
import com.rentdrive.enums.Transmission;
import com.rentdrive.enums.VehicleCategory;
import com.rentdrive.enums.VehicleStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface VehicleService {

    VehicleResponse create(UUID ownerId, CreateVehicleRequest request);

    List<VehicleSummaryResponse> getMyVehicles(UUID ownerId);

    VehicleResponse getById(UUID vehicleId);

    VehicleResponse update(UUID ownerId, UUID vehicleId, UpdateVehicleRequest request);

    void delete(UUID ownerId, UUID vehicleId);

    VehicleResponse addPhoto(UUID ownerId, UUID vehicleId, AddPhotoRequest request);

    VehicleResponse deletePhoto(UUID ownerId, UUID vehicleId, UUID photoId);

    // Public catalog
    PageResponse<VehicleSummaryResponse> search(String wilaya, VehicleCategory category,
                                                Transmission transmission, FuelType fuelType,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                Pageable pageable);

    // Admin
    VehicleResponse updateStatus(UUID vehicleId, VehicleStatus status);

    PageResponse<VehicleSummaryResponse> listAll(VehicleStatus status, Pageable pageable);
}
