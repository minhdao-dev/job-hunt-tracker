package com.jobhunt.tracker.module.reminder.repository;

import com.jobhunt.tracker.module.reminder.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    @Query("""
            SELECT r FROM Reminder r
            WHERE r.job.id = :jobId
              AND r.job.user.id = :userId
              AND r.deletedAt IS NULL
            ORDER BY r.remindAt ASC
            """)
    List<Reminder> findAllByJobIdAndUserId(UUID jobId, UUID userId);

    @Query("""
            SELECT r FROM Reminder r
            WHERE r.id = :id
              AND r.job.id = :jobId
              AND r.job.user.id = :userId
              AND r.deletedAt IS NULL
            """)
    Optional<Reminder> findByIdAndJobIdAndUserId(UUID id, UUID jobId, UUID userId);

    @Query("""
            SELECT r FROM Reminder r
            WHERE r.isSent = false
              AND r.remindAt <= :now
              AND r.deletedAt IS NULL
            """)
    List<Reminder> findPendingReminders(LocalDateTime now);

    @Query("""
        SELECT COUNT(r) FROM Reminder r
        WHERE r.job.user.id = :userId
          AND r.isSent = false
          AND r.deletedAt IS NULL
        """)
    long countPendingByUserId(UUID userId);
}