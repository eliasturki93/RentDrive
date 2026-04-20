package com.rentdrive.repository;

import com.rentdrive.entity.Review;
import com.rentdrive.enums.ReviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByBookingIdAndType(UUID bookingId, ReviewType type);

    @EntityGraph(attributePaths = {"author", "author.profile"})
    @Query("SELECT r FROM Review r WHERE r.vehicle.id = :vehicleId ORDER BY r.createdAt DESC")
    Page<Review> findByVehicleId(@Param("vehicleId") UUID vehicleId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "author.profile"})
    @Query("SELECT r FROM Review r WHERE r.booking.store.id = :storeId ORDER BY r.createdAt DESC")
    Page<Review> findByStoreId(@Param("storeId") UUID storeId, Pageable pageable);
}
