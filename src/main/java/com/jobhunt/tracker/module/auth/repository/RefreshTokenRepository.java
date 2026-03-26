package com.jobhunt.tracker.module.auth.repository;

import com.jobhunt.tracker.module.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("""
            SELECT r FROM RefreshToken r
            WHERE r.token = :token
              AND r.isRevoked = false
            """)
    Optional<RefreshToken> findValidToken(String token);

    // Revoke tất cả token của user — dùng khi logout all devices
    @Modifying
    @Query("""
            UPDATE RefreshToken r
            SET r.isRevoked = true
            WHERE r.user.id = :userId
              AND r.isRevoked = false
            """)
    void revokeAllByUserId(UUID userId);

    // Xóa token hết hạn — dùng cho scheduled cleanup
    @Modifying
    @Query("""
            DELETE FROM RefreshToken r
            WHERE r.expiresAt < CURRENT_TIMESTAMP
               OR r.isRevoked = true
            """)
    void deleteExpiredAndRevoked();
}