package com.kinplatform.auth.password;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de {@link PasswordResetToken}. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :usedAt "
            + "where t.userId = :userId and t.usedAt is null")
    int markUsedForUser(@Param("userId") UUID userId, @Param("usedAt") java.time.OffsetDateTime usedAt);
}
