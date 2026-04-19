package com.rentdrive.entity;

import com.rentdrive.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Contient uniquement les credentials d'authentification.
 * Les données personnelles (PII) sont isolées dans Profile.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_phone", columnNames = "phone")
    },
    indexes = @Index(name = "idx_users_status", columnList = "status")
)
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relations ─────────────────────────────────────────────────────────────

    /**
     * Relation 1:1 — Profile est le côté propriétaire (detient la FK user_id).
     * cascade = ALL : créer/supprimer un User entraîne Profile.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY, optional = false)
    private Profile profile;

    /**
     * Rôles N:M via table de jonction user_roles.
     * FetchType.LAZY pour ne pas charger tous les rôles à chaque requête User.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Un User peut gérer au plus 1 Store (contrainte UK sur owner_id).
     * Relation inverse — Store est le côté propriétaire.
     */
    @OneToOne(mappedBy = "owner", fetch = FetchType.LAZY)
    private Store store;

    /** Toutes les réservations effectuées par ce locataire. */
    @OneToMany(mappedBy = "renter", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    /** Documents KYC soumis. Supprimés en cascade avec le compte. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Document> documents = new ArrayList<>();

    /** Avis rédigés (en tant qu'auteur). */
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
}
