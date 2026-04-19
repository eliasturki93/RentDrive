package com.rentdrive.repository;

import com.rentdrive.entity.User;
import com.rentdrive.enums.BookingStatus;
import com.rentdrive.enums.RoleName;
import com.rentdrive.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour l'entité User.
 *
 * Conventions :
 *  - Lectures simples          → Spring Data derived query methods
 *  - JOIN / logique complexe   → @Query JPQL
 *  - Mises à jour bulk         → @Modifying + @Query (UPDATE SQL direct)
 *  - Chargement de relations   → @EntityGraph pour éviter les N+1 queries
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // =========================================================================
    // LOOKUPS SIMPLES — Spring Data derived queries (aucun JPQL nécessaire)
    // =========================================================================

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /** Unicité email en excluant le user courant — pour les mises à jour de profil. */
    boolean existsByEmailAndIdNot(String email, UUID id);

    /** Unicité téléphone en excluant le user courant. */
    boolean existsByPhoneAndIdNot(String phone, UUID id);

    List<User> findByStatus(UserStatus status);

    // =========================================================================
    // CHARGEMENT AVEC RELATIONS — @EntityGraph (1 JOIN = 0 N+1 problem)
    // =========================================================================

    /**
     * Charge User + Profile + Roles en un seul JOIN SQL.
     *
     * Sans @EntityGraph : 3 SELECT séparés (user, profile, roles).
     * Avec @EntityGraph : 1 seul LEFT JOIN → ~3× plus rapide.
     * Usage : authentification, endpoint /me.
     */
    @EntityGraph(attributePaths = {"profile", "roles"})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithProfileAndRoles(@Param("email") String email);

    /**
     * User + toutes relations — pour la fiche admin complète.
     * NE PAS utiliser sur des listes : JOIN exponentiel si N users.
     */
    @EntityGraph(attributePaths = {"profile", "roles", "store", "documents"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithAllRelations(@Param("id") UUID id);

    /**
     * User + Profile uniquement — cas le plus fréquent.
     */
    @EntityGraph(attributePaths = {"profile"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithProfile(@Param("id") UUID id);

    // =========================================================================
    // REQUÊTES MÉTIER — JPQL
    // =========================================================================

    /**
     * Tous les utilisateurs actifs ayant un rôle donné.
     * Usage : lister les AGENCE en attente de vérification KYC.
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            WHERE r.name = :roleName
              AND u.status = 'ACTIVE'
            ORDER BY u.createdAt DESC
            """)
    List<User> findActiveUsersByRole(@Param("roleName") RoleName roleName);

    /**
     * Recherche full-text sur prénom, nom, email.
     * Paginée pour le backoffice admin (liste users).
     */
    @Query("""
            SELECT u FROM User u
            JOIN u.profile p
            WHERE u.status != 'BANNED'
              AND (
                LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY p.lastName ASC, p.firstName ASC
            """)
    Page<User> searchUsers(@Param("q") String query, Pageable pageable);

    /**
     * Nombre d'inscriptions dans un intervalle — métriques admin dashboard.
     */
    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.createdAt BETWEEN :from AND :to
            """)
    long countCreatedBetween(@Param("from") LocalDateTime from,
                             @Param("to")   LocalDateTime to);

    /**
     * Users dont email OU téléphone non vérifié, compte > 24h.
     * Pour les jobs de relance de vérification (scheduled task).
     */
    @Query("""
            SELECT u FROM User u
            WHERE (u.emailVerified = false OR u.phoneVerified = false)
              AND u.status = 'ACTIVE'
              AND u.createdAt < :threshold
            ORDER BY u.createdAt ASC
            """)
    List<User> findUnverifiedOlderThan(@Param("threshold") LocalDateTime threshold);

    /**
     * Vérifie si un user possède un rôle précis.
     * Plus efficace qu'un findById() + stream filter : 1 COUNT SQL.
     */
    @Query("""
            SELECT COUNT(u) > 0 FROM User u
            JOIN u.roles r
            WHERE u.id = :userId
              AND r.name = :roleName
            """)
    boolean hasRole(@Param("userId") UUID userId,
                    @Param("roleName") RoleName roleName);

    /**
     * Nombre de stores possédés par un user.
     * Valide qu'un user ne peut pas créer un second store (règle métier).
     */
    @Query("""
            SELECT COUNT(s) FROM Store s
            WHERE s.owner.id = :userId
            """)
    long countStoresByOwner(@Param("userId") UUID userId);

    /**
     * Users avec des documents KYC en attente de validation.
     * Pour la file de vérification admin.
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.documents d
            WHERE d.status = 'PENDING'
              AND u.status = 'ACTIVE'
            ORDER BY u.createdAt ASC
            """)
    List<User> findUsersWithPendingDocuments();

    // =========================================================================
    // MISES À JOUR BULK — @Modifying (UPDATE SQL direct, sans cache JPA)
    // =========================================================================

    /**
     * Mise à jour de statut en masse — ban/suspend par l'admin.
     * Retourne le nombre de lignes affectées.
     */
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id IN :ids")
    int updateStatusBulk(@Param("ids")    List<UUID> ids,
                         @Param("status") UserStatus status);

    /**
     * Valide l'email après clic sur le lien d'activation.
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :id")
    int markEmailVerified(@Param("id") UUID id);

    /**
     * Valide le téléphone après saisie du code OTP.
     */
    @Modifying
    @Query("UPDATE User u SET u.phoneVerified = true WHERE u.id = :id")
    int markPhoneVerified(@Param("id") UUID id);

    /**
     * Mise à jour du hash après reset ou changement de mot de passe.
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :hash WHERE u.id = :id")
    int updatePasswordHash(@Param("id")   UUID id,
                           @Param("hash") String hash);

    /**
     * Vérifie si un user possède des réservations actives (PENDING, CONFIRMED, IN_PROGRESS).
     * Utilisé avant la suppression de compte.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.renter.id = :userId
              AND b.status IN :statuses
            """)
    boolean hasActiveBookings(@Param("userId")   UUID userId,
                              @Param("statuses") List<BookingStatus> statuses);
}
