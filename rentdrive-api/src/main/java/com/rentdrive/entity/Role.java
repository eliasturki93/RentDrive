package com.rentdrive.entity;

import com.rentdrive.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

/**
 * Table de référence des rôles applicatifs.
 * Seed à l'init : INSERT INTO roles (name) VALUES ('LOCATAIRE'),('BAILLEUR'),('AGENCE'),('ADMIN').
 */
@Entity
@Table(
    name = "roles",
    uniqueConstraints = @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
)
@Getter @Setter @NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleName name;

    @Column(length = 255)
    private String description;

    /**
     * Côté inverse de la relation N:M User ↔ Role.
     * mappedBy pointe sur le champ "roles" de User.
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
}
