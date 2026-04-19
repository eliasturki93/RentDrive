package com.rentdrive.entity;


import com.rentdrive.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Entité pivot de la plateforme — toutes les transactions convergent ici.
 *
 * State machine :
 *   PENDING → CONFIRMED → IN_PROGRESS → COMPLETED
 *          ↘ CANCELLED              ↘ DISPUTED
 *
 * @Version (optimistic locking) : protège contre les transitions d'état
 * concurrentes (ex: deux annulations simultanées).
 *
 * Index composite (vehicle_id, start_date, end_date) : requête critique
 * pour détecter les conflits de disponibilité en O(log n).
 */
@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_bookings_vehicle_dates",
               columnList = "vehicle_id, start_date, end_date"),
        @Index(name = "idx_bookings_renter",  columnList = "renter_id"),
        @Index(name = "idx_bookings_status",  columnList = "status"),
        @Index(name = "idx_bookings_created", columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    /**
     * Verrou optimiste — empêche deux transitions d'état simultanées
     * sur la même réservation (ex: pattern SAGA compensation).
     */
    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "vehicle_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_bookings_vehicle")
    )
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "renter_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_bookings_renter")
    )
    private User renter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "store_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_bookings_store")
    )
    private Store store;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private Short totalDays;

    /** Snapshot du tarif au moment de la réservation — immuable. */
    @Column(name = "price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /** Commission plateforme (10-14% selon le profil). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal commission;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relations ─────────────────────────────────────────────────────────────

    /**
     * Composition 1:1 — chaque réservation génère exactement 1 paiement.
     * cascade = ALL : créer un Booking avec Payment persiste les deux.
     */
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY)
    private Payment payment;

    /**
     * Max 2 avis par réservation : RENTER_TO_OWNER + OWNER_TO_RENTER.
     * Contrainte UNIQUE (booking_id, type) garantie en DB.
     */
    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
}
