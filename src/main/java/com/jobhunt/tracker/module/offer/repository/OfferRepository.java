package com.jobhunt.tracker.module.offer.repository;

import com.jobhunt.tracker.module.offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @Query("""
            SELECT o FROM Offer o
            WHERE o.job.id = :jobId
              AND o.job.user.id = :userId
              AND o.deletedAt IS NULL
            """)
    Optional<Offer> findByJobIdAndUserId(UUID jobId, UUID userId);

    @Query("""
            SELECT COUNT(o) > 0 FROM Offer o
            WHERE o.job.id = :jobId
              AND o.deletedAt IS NULL
            """)
    boolean existsByJobId(UUID jobId);
}