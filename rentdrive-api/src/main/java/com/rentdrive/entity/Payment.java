package com.rentdrive.entity;

import com.rentdrive.converter.JsonMapConverter;
import com.rentdrive.enums.DepositStatus;
import com.rentdrive.enums.PaymentMethod;
import com.rentdrive.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Un paiement par réservation (UNIQUE KEY sur booking_id).
 *
 * Cycle de vie de la caution (depositStatus) :
 *   HELD → RELEASED : fin de location normale, caution débloquée.
 *   HELD → FORFEITED : litige résolu en faveur du bailleur.
 *
 * metadata (JSON) : stocke les données retournées par la passerelle
 * de paiement (CIB response code, transaction timestamp, etc.).
 */
@Entity
@Table(
    name = "payments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_payments_booking", columnNames = "booking_id"),
    indexes = {
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_method", columnList = "method")
    }
)
@Getter @Setter @NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    /**
     * Côté propriétaire de la relation 1:1 avec Booking.
     * Détient la FK booking_id.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "booking_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_payments_booking")
    )
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** ISO 4217 — DZD par défaut pour l'Algérie. */
    @Column(nullable = false, length = 3)
    private String currency = "DZD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false, length = 15)
    private DepositStatus depositStatus = DepositStatus.HELD;

    /** Référence retournée par la passerelle CIB/Edahabia. */
    @Column(name = "transaction_ref", length = 255)
    private String transactionRef;

    /** Payload brut de la réponse passerelle — pour audit et support. */
    @Column(columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
