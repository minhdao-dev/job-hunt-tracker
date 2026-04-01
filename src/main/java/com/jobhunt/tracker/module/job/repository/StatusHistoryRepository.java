package com.jobhunt.tracker.module.job.repository;

import com.jobhunt.tracker.module.job.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

    @Query("""
            SELECT h FROM StatusHistory h
            WHERE h.job.id = :jobId
            ORDER BY h.changedAt ASC
            """)
    List<StatusHistory> findAllByJobId(UUID jobId);
}