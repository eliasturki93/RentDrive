package com.rentdrive.entity;

import com.rentdrive.converter.JsonMapConverter;
import com.rentdrive.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Catalogue des véhicules.
 *
 * Champ features (JSON) : stocke les équipements variables selon la catégorie
 * Ex: {"clim": true, "gps": true, "bluetooth": true, "sieges_bebe": 1}
 *
 * @Version (optimistic locking) : prévient les race conditions lors des
 * modifications concurrentes de statut/disponibilité.
 */
@Entity
@Table(
    name = "vehicles",
    indexes = {
        @Index(name = "idx_vehicles_category", columnList = "category"),
        @Index(name = "idx_vehicles_status",   columnList = "status"),
        @Index(name = "idx_vehicles_wilaya",   columnList = "wilaya"),
        @Index(name = "idx_vehicles_price",    columnList = "price_per_day")
    }
)
@Getter @Setter @NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    /**
     * Verrou optimiste — empêche deux requêtes simultanées de modifier
     * le même véhicule (ex: changement de statut AVAILABLE → RENTED).
     */
    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "store_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_vehicles_store")
    )
    private Store store;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false)
    private Short year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private VehicleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Transmission transmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 15)
    private FuelType fuelType;

    @Column(nullable = false)
    private Byte seats;

    private Integer mileage;

    @Column(name = "price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "price_per_week", precision = 10, scale = 2)
    private BigDecimal pricePerWeek;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    /**
     * Équipements en JSON — structure variable selon la catégorie.
     * Ne pas utiliser @ElementCollection (table séparée) : trop coûteux pour des flags simples.
     */
    @Column(columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> features;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status = VehicleStatus.PENDING_REVIEW;

    /** Coordonnées GPS — DECIMAL(10,8) = précision ~1mm. */
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(length = 100)
    private String wilaya;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relations ─────────────────────────────────────────────────────────────

    /**
     * Photos triées par orderIndex ASC.
     * orphanRemoval = true : retirer une photo de la liste la supprime en DB.
     */
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<VehiclePhoto> photos = new ArrayList<>();
}
