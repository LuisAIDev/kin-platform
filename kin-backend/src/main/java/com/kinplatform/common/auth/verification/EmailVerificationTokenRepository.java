package com.kinplatform.common.auth.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Invalida (marca como usados) los tokens previos sin usar de un usuario. */
    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = :usedAt where t.userId = :userId and t.usedAt is null")
    void markAllUsedForUser(@Param("userId") UUID userId, @Param("usedAt") OffsetDateTime usedAt);
}

