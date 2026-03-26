package com.jobhunt.tracker.module.auth.repository;

import com.jobhunt.tracker.module.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    @Query("""
            SELECT o FROM OtpToken o
            WHERE o.token = :token
              AND o.type = :type
              AND o.isUsed = false
            """)
    Optional<OtpToken> findValidToken(String token, OtpToken.OtpType type);

    @Modifying
    @Query("""
            UPDATE OtpToken o
            SET o.isUsed = true
            WHERE o.user.id = :userId
              AND o.type = :type
              AND o.isUsed = false
            """)
    void invalidateAllByUserIdAndType(UUID userId, OtpToken.OtpType type);
    
    @Modifying
    @Query("""
            DELETE FROM OtpToken o
            WHERE o.expiresAt < CURRENT_TIMESTAMP
               OR o.isUsed = true
            """)
    void deleteExpiredAndUsed();
}