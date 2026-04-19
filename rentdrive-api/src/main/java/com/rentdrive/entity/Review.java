package com.rentdrive.entity;

import com.rentdrive.enums.ReviewType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Avis bilatéraux post-location.
 *
 * UNIQUE (booking_id, type) : garantit qu'on ne peut pas avoir deux avis
 * dans le même sens (RENTER_TO_OWNER ou OWNER_TO_RENTER) pour la même réservation.
 *
 * Les deux champs author et target référencent User :
 *   - author : celui qui rédige l'avis
 *   - target : celui qui reçoit l'avis (mis à jour dans Store.rating / User.trustScore)
 */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reviews_booking_type", columnNames = {"booking_id", "type"}),
    indexes = {
        @Index(name = "idx_reviews_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_reviews_target",  columnList = "target_id")
    }
)
@Getter @Setter @NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "booking_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_reviews_booking")
    )
    private Booking booking;

    /** Auteur de l'avis (locataire ou bailleur). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "author_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_reviews_author")
    )
    private User author;

    /** Destinataire de l'avis — sa note agrégée est mise à jour en conséquence. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "target_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_reviews_target")
    )
    private User target;

    /** Véhicule concerné — permet d'afficher les avis par véhicule. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "vehicle_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_reviews_vehicle")
    )
    private Vehicle vehicle;

    /** Note de 1 à 5. Validée au niveau Bean Validation ET CHECK SQL. */
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Byte rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ReviewType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
