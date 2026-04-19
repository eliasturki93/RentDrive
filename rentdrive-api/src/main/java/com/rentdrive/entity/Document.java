package com.rentdrive.entity;

import com.rentdrive.enums.DocumentType;
import com.rentdrive.enums.VerifStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pièces justificatives KYC soumises par les utilisateurs.
 *
 * Cycle de validation :
 *   PENDING → VERIFIED : document validé par un admin (verifiedBy).
 *   PENDING → REJECTED : document refusé, raison dans rejectionReason.
 *
 * ON DELETE CASCADE depuis User : les documents sont supprimés
 * automatiquement à la suppression du compte.
 */
@Entity
@Table(
    name = "documents",
    indexes = {
        @Index(name = "idx_documents_user_type", columnList = "user_id, type"),
        @Index(name = "idx_documents_status",    columnList = "status")
    }
)
@Getter @Setter @NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_documents_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType type;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private VerifStatus status = VerifStatus.PENDING;

    /**
     * UUID de l'admin ayant validé/rejeté — non mappé en @ManyToOne
     * pour éviter un chargement inutile de l'entité admin à chaque lecture.
     */
    @Column(name = "verified_by", columnDefinition = "CHAR(36)")
    private UUID verifiedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Date d'expiration (obligatoire pour permis et carte d'identité). */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
