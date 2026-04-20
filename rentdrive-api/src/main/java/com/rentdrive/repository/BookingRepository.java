package com.rentdrive.repository;

import com.rentdrive.entity.Booking;
import com.rentdrive.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** Booking complet avec toutes les relations nécessaires pour le mapper. */
    @EntityGraph(attributePaths = {
            "vehicle", "vehicle.photos",
            "renter", "renter.profile",
            "store",
            "payment"
    })
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") UUID id);

    /** Réservations du locataire connecté. */
    @EntityGraph(attributePaths = {"vehicle", "vehicle.photos", "renter", "renter.profile", "store"})
    @Query("SELECT b FROM Booking b WHERE b.renter.id = :renterId ORDER BY b.createdAt DESC")
    Page<Booking> findByRenterId(@Param("renterId") UUID renterId, Pageable pageable);

    /** Réservations reçues par un store. */
    @EntityGraph(attributePaths = {"vehicle", "vehicle.photos", "renter", "renter.profile", "store"})
    @Query("""
            SELECT b FROM Booking b
            WHERE b.store.id = :storeId
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findByStoreId(@Param("storeId") UUID storeId,
                                @Param("status")  BookingStatus status,
                                Pageable pageable);

    /** Admin — toutes les réservations. */
    @EntityGraph(attributePaths = {"vehicle", "vehicle.photos", "renter", "renter.profile", "store"})
    @Query("""
            SELECT b FROM Booking b
            WHERE (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findAllForAdmin(@Param("status") BookingStatus status, Pageable pageable);

    /**
     * Détection de chevauchement — requête critique pour la disponibilité.
     * Utilise l'index composite (vehicle_id, start_date, end_date).
     *
     * Chevauchement si : startDate < endExisting AND endDate > startExisting
     * Statuts bloquants : PENDING, CONFIRMED, IN_PROGRESS
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.vehicle.id = :vehicleId
              AND b.status IN :statuses
              AND b.startDate < :endDate
              AND b.endDate   > :startDate
            """)
    boolean hasOverlap(@Param("vehicleId") UUID vehicleId,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate")   LocalDate endDate,
                       @Param("statuses")  List<BookingStatus> statuses);

    /** Vérifie que le locataire est bien l'auteur de cette réservation. */
    boolean existsByIdAndRenterId(UUID bookingId, UUID renterId);

    /** Vérifie que la réservation appartient à un store de cet owner. */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.id = :bookingId
              AND b.store.owner.id = :ownerId
            """)
    boolean existsByIdAndStoreOwnerId(@Param("bookingId") UUID bookingId,
                                      @Param("ownerId")   UUID ownerId);
}
