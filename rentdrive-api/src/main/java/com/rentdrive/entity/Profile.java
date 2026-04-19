package com.rentdrive.entity;

import com.rentdrive.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Données personnelles identifiables (PII) séparées de User.
 * Permet une gestion granulaire des droits d'accès et du RGPD.
 */
@Entity
@Table(
    name = "profiles",
    uniqueConstraints = @UniqueConstraint(name = "uk_profiles_user_id", columnNames = "user_id")
)
@Getter @Setter @NoArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    /**
     * Côté propriétaire de la relation 1:1 avec User.
     * Détient la FK user_id en base de données.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_profiles_user")
    )
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 100)
    private String wilaya;

    @Column(length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;
}
