package com.rentdrive.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Photos d'un véhicule, ordonnées par orderIndex.
 * Supprimées en cascade avec le véhicule (orphanRemoval dans Vehicle).
 */
@Entity
@Table(
    name = "vehicle_photos",
    indexes = @Index(name = "idx_photos_vehicle", columnList = "vehicle_id")
)
@Getter @Setter @NoArgsConstructor
public class VehiclePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "vehicle_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_photos_vehicle")
    )
    private Vehicle vehicle;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    /** Ordre d'affichage — photo principale généralement en position 0. */
    @Column(name = "order_index", nullable = false)
    private Byte orderIndex = 0;
}
