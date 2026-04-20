package com.rentdrive.repository;

import com.rentdrive.entity.Store;
import com.rentdrive.enums.StoreStatus;
import com.rentdrive.enums.StoreType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    boolean existsByOwnerId(UUID ownerId);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    @Query("SELECT s FROM Store s WHERE s.id = :id")
    Optional<Store> findByIdWithOwner(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    @Query("SELECT s FROM Store s WHERE s.owner.id = :ownerId")
    Optional<Store> findByOwnerIdWithOwner(@Param("ownerId") UUID ownerId);

    /** Liste publique — uniquement les stores APPROVED. */
    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    @Query("""
            SELECT s FROM Store s
            WHERE s.status = 'APPROVED'
              AND (:wilaya IS NULL OR LOWER(s.wilaya) = LOWER(:wilaya))
              AND (:type   IS NULL OR s.type = :type)
            ORDER BY s.rating DESC, s.createdAt DESC
            """)
    Page<Store> findApproved(@Param("wilaya") String wilaya,
                             @Param("type")   StoreType type,
                             Pageable pageable);

    /** Liste admin — tous les stores avec filtre optionnel sur le statut. */
    @EntityGraph(attributePaths = {"owner", "owner.profile"})
    @Query("""
            SELECT s FROM Store s
            WHERE (:status IS NULL OR s.status = :status)
            ORDER BY s.createdAt DESC
            """)
    Page<Store> findAllForAdmin(@Param("status") StoreStatus status,
                                Pageable pageable);
}
