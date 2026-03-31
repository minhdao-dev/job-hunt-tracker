package com.jobhunt.tracker.module.company.repository;

import com.jobhunt.tracker.module.company.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    @Query("""
            SELECT c FROM Company c
            WHERE c.user.id = :userId
              AND c.deletedAt IS NULL
            ORDER BY c.name ASC
            """)
    Page<Company> findAllByUserId(UUID userId, Pageable pageable);

    @Query("""
            SELECT c FROM Company c
            WHERE c.user.id = :userId
              AND c.deletedAt IS NULL
              AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY c.name ASC
            """)
    Page<Company> searchByName(UUID userId, String keyword, Pageable pageable);

    @Query("""
            SELECT c FROM Company c
            WHERE c.id = :id
              AND c.user.id = :userId
              AND c.deletedAt IS NULL
            """)
    Optional<Company> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT COUNT(c) > 0 FROM Company c
            WHERE c.user.id = :userId
              AND LOWER(c.name) = LOWER(:name)
              AND c.deletedAt IS NULL
            """)
    boolean existsByUserIdAndName(UUID userId, String name);

    @Query("""
            SELECT COUNT(c) > 0 FROM Company c
            WHERE c.user.id = :userId
              AND LOWER(c.name) = LOWER(:name)
              AND c.id <> :excludeId
              AND c.deletedAt IS NULL
            """)
    boolean existsByUserIdAndNameExcluding(UUID userId, String name, UUID excludeId);
}