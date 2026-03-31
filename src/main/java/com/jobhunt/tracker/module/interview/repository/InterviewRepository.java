package com.jobhunt.tracker.module.interview.repository;

import com.jobhunt.tracker.module.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    @Query("""
            SELECT i FROM Interview i
            WHERE i.job.id = :jobId
              AND i.job.user.id = :userId
              AND i.deletedAt IS NULL
            ORDER BY i.scheduledAt ASC
            """)
    List<Interview> findAllByJobIdAndUserId(UUID jobId, UUID userId);

    @Query("""
            SELECT i FROM Interview i
            WHERE i.id = :id
              AND i.job.id = :jobId
              AND i.job.user.id = :userId
              AND i.deletedAt IS NULL
            """)
    Optional<Interview> findByIdAndJobIdAndUserId(UUID id, UUID jobId, UUID userId);

    @Query("""
            SELECT i.result, COUNT(i) FROM Interview i
            WHERE i.job.user.id = :userId
              AND i.deletedAt IS NULL
            GROUP BY i.result
            """)
    List<Object[]> countByResult(UUID userId);

    @Query("""
            SELECT i.interviewType, COUNT(i) FROM Interview i
            WHERE i.job.user.id = :userId
              AND i.deletedAt IS NULL
            GROUP BY i.interviewType
            """)
    List<Object[]> countByType(UUID userId);

    @Query("""
            SELECT COUNT(DISTINCT i.job.id) FROM Interview i
            WHERE i.job.user.id = :userId
              AND i.deletedAt IS NULL
            """)
    long countDistinctJobsWithInterview(UUID userId);
}