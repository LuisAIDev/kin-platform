package com.kinplatform.kin.health.common.repository;

import com.kinplatform.kin.health.common.entity.UserConsent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    List<UserConsent> findByUserId(UUID userId);

    Optional<UserConsent> findByUserIdAndConsentTypeAndVersion(
            UUID userId, UserConsent.ConsentType consentType, String version);

    List<UserConsent> findByUserIdAndConsentType(UUID userId, UserConsent.ConsentType consentType);

    @Query(
            "SELECT uc FROM UserConsent uc WHERE uc.userId = :userId AND uc.consentType = :consentType AND uc.accepted = TRUE AND uc.revokedAt IS NULL")
    Optional<UserConsent> findActiveConsent(
            @Param("userId") UUID userId, @Param("consentType") UserConsent.ConsentType consentType);

    List<UserConsent> findByUserIdAndAcceptedTrue(UUID userId);

    long countByUserIdAndConsentTypeAndAcceptedTrue(UUID userId, UserConsent.ConsentType consentType);
}
