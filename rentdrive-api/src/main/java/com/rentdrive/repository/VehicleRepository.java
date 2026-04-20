package com.rentdrive.repository;

import com.rentdrive.entity.Vehicle;
import com.rentdrive.enums.FuelType;
import com.rentdrive.enums.Transmission;
import com.rentdrive.enums.VehicleCategory;
import com.rentdrive.enums.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    /** Véhicule complet avec store et photos — pour les endpoints détail. */
    @EntityGraph(attributePaths = {"store", "photos"})
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdWithDetails(@Param("id") UUID id);

    /** Tous les véhicules d'un store (propriétaire). */
    @EntityGraph(attributePaths = {"store", "photos"})
    @Query("SELECT v FROM Vehicle v WHERE v.store.id = :storeId ORDER BY v.createdAt DESC")
    List<Vehicle> findByStoreId(@Param("storeId") UUID storeId);

    /** Vérifie que le véhicule appartient bien au store de cet owner. */
    @Query("""
            SELECT COUNT(v) > 0 FROM Vehicle v
            WHERE v.id = :vehicleId
              AND v.store.owner.id = :ownerId
            """)
    boolean existsByIdAndStoreOwnerId(@Param("vehicleId") UUID vehicleId,
                                      @Param("ownerId")   UUID ownerId);

    /**
     * Catalogue public — véhicules AVAILABLE dans des stores APPROVED.
     * Filtres tous optionnels.
     */
    @EntityGraph(attributePaths = {"store", "photos"})
    @Query("""
            SELECT v FROM Vehicle v
            JOIN v.store s
            WHERE v.status = 'AVAILABLE'
              AND s.status = 'APPROVED'
              AND (:wilaya   IS NULL OR LOWER(v.wilaya) = LOWER(:wilaya))
              AND (:category IS NULL OR v.category = :category)
              AND (:transmission IS NULL OR v.transmission = :transmission)
              AND (:fuelType IS NULL OR v.fuelType = :fuelType)
              AND (:minPrice IS NULL OR v.pricePerDay >= :minPrice)
              AND (:maxPrice IS NULL OR v.pricePerDay <= :maxPrice)
            ORDER BY v.pricePerDay ASC
            """)
    Page<Vehicle> findAvailable(
            @Param("wilaya")       String          wilaya,
            @Param("category")     VehicleCategory category,
            @Param("transmission") Transmission    transmission,
            @Param("fuelType")     FuelType        fuelType,
            @Param("minPrice")     BigDecimal      minPrice,
            @Param("maxPrice")     BigDecimal      maxPrice,
            Pageable pageable);

    /** Liste admin — tous statuts, filtre optionnel. */
    @EntityGraph(attributePaths = {"store", "photos"})
    @Query("""
            SELECT v FROM Vehicle v
            WHERE (:status IS NULL OR v.status = :status)
            ORDER BY v.createdAt DESC
            """)
    Page<Vehicle> findAllForAdmin(@Param("status") VehicleStatus status, Pageable pageable);
}
