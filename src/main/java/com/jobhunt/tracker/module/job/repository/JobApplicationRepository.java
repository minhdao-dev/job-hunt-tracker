package com.jobhunt.tracker.module.job.repository;

import com.jobhunt.tracker.module.job.entity.JobApplication;
import com.jobhunt.tracker.module.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    @Query("""
            SELECT j FROM JobApplication j
            LEFT JOIN FETCH j.company
            WHERE j.user.id = :userId
              AND j.deletedAt IS NULL
            """)
    Page<JobApplication> findAllByUserId(UUID userId, Pageable pageable);

    @Query("""
            SELECT j FROM JobApplication j
            LEFT JOIN FETCH j.company
            WHERE j.user.id = :userId
              AND j.status = :status
              AND j.deletedAt IS NULL
            """)
    Page<JobApplication> findAllByUserIdAndStatus(
            UUID userId, JobStatus status, Pageable pageable);

    @Query("""
            SELECT j FROM JobApplication j
            LEFT JOIN FETCH j.company
            WHERE j.user.id = :userId
              AND j.deletedAt IS NULL
              AND (
                LOWER(j.position) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(j.company.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<JobApplication> searchByKeyword(UUID userId, String keyword, Pageable pageable);

    @Query("""
            SELECT j FROM JobApplication j
            LEFT JOIN FETCH j.company
            WHERE j.id = :id
              AND j.user.id = :userId
              AND j.deletedAt IS NULL
            """)
    Optional<JobApplication> findByIdAndUserId(UUID id, UUID userId);
}