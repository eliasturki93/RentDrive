package com.rentdrive.repository;

import com.rentdrive.entity.Document;
import com.rentdrive.enums.VerifStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"user", "user.profile"})
    @Query("""
            SELECT d FROM Document d
            WHERE (:status IS NULL OR d.status = :status)
            ORDER BY d.createdAt ASC
            """)
    Page<Document> findAllForAdmin(@Param("status") VerifStatus status, Pageable pageable);
}
