package com.rentdrive.repository;

import com.rentdrive.entity.VehiclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, UUID> {

    int countByVehicleId(UUID vehicleId);

    /** Retire le flag isPrimary de toutes les photos du véhicule. */
    @Modifying
    @Query("UPDATE VehiclePhoto p SET p.isPrimary = false WHERE p.vehicle.id = :vehicleId")
    void clearPrimaryFlag(@Param("vehicleId") UUID vehicleId);
}
