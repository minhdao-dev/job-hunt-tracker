package com.jobhunt.tracker.module.user.repository;

import com.jobhunt.tracker.module.user.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    @Query("SELECT s FROM UserSettings s WHERE s.user.id = :userId AND s.deletedAt IS NULL")
    Optional<UserSettings> findByUserId(UUID userId);

    @Query("SELECT COUNT(s) > 0 FROM UserSettings s WHERE s.user.id = :userId AND s.deletedAt IS NULL")
    boolean existsByUserId(UUID userId);
}