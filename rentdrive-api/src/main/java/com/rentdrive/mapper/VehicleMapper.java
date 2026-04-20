package com.rentdrive.mapper;

import com.rentdrive.dto.response.VehicleResponse;
import com.rentdrive.dto.response.VehicleSummaryResponse;
import com.rentdrive.entity.Vehicle;
import com.rentdrive.entity.VehiclePhoto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VehicleMapper {

    public VehicleResponse toResponse(Vehicle v) {
        List<VehicleResponse.PhotoInfo> photos = v.getPhotos().stream()
                .map(p -> new VehicleResponse.PhotoInfo(
                        p.getId(), p.getUrl(), p.isPrimary(), p.getOrderIndex()))
                .toList();

        VehicleResponse.StoreInfo store = new VehicleResponse.StoreInfo(
                v.getStore().getId(),
                v.getStore().getName(),
                v.getStore().getWilaya());

        return new VehicleResponse(
                v.getId(), v.getBrand(), v.getModel(), v.getYear(),
                v.getCategory(), v.getTransmission(), v.getFuelType(),
                v.getSeats(), v.getMileage(),
                v.getPricePerDay(), v.getPricePerWeek(), v.getDepositAmount(),
                v.getFeatures(), v.getDescription(), v.getStatus(),
                v.getLatitude(), v.getLongitude(), v.getWilaya(),
                photos, store, v.getCreatedAt());
    }

    public VehicleSummaryResponse toSummary(Vehicle v) {
        String primaryPhoto = v.getPhotos().stream()
                .filter(VehiclePhoto::isPrimary)
                .map(VehiclePhoto::getUrl)
                .findFirst()
                .orElse(v.getPhotos().isEmpty() ? null : v.getPhotos().get(0).getUrl());

        return new VehicleSummaryResponse(
                v.getId(), v.getBrand(), v.getModel(), v.getYear(),
                v.getCategory(), v.getTransmission(), v.getFuelType(),
                v.getPricePerDay(), v.getStatus(),
                primaryPhoto, v.getWilaya(),
                v.getStore().getId(), v.getStore().getName());
    }
}
