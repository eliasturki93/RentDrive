package com.rentdrive.entity;

import com.rentdrive.enums.StoreStatus;
import com.rentdrive.enums.StoreType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Représente une agence professionnelle (AGENCY) ou un bailleur privé (PRIVATE).
 * Contrainte : 1 User = max 1 Store (UNIQUE KEY sur owner_id).
 */
@Entity
@Table(
    name = "stores",
    uniqueConstraints = @UniqueConstraint(name = "uk_stores_owner", columnNames = "owner_id"),
    indexes = {
        @Index(name = "idx_stores_wilaya", columnList = "wilaya"),
        @Index(name = "idx_stores_verif",  columnList = "verification_status")
    }
)
@Getter @Setter @NoArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    /**
     * Côté propriétaire de la relation 1:1 avec User.
     * UNIQUE KEY garantit qu'un user ne peut avoir qu'un seul store.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "owner_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_stores_owner")
    )
    private User owner;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StoreType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 15)
    private StoreStatus status = StoreStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String wilaya;

    /** Moyenne des avis — mise à jour par ReviewService après chaque avis. */
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relations ─────────────────────────────────────────────────────────────

    /**
     * Composition : les véhicules appartiennent au store.
     * orphanRemoval = true : retirer un véhicule de la liste le supprime en DB.
     */
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Vehicle> vehicles = new ArrayList<>();
}
