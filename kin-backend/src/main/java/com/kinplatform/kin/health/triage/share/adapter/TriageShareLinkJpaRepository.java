package com.kinplatform.kin.health.triage.share.adapter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de enlaces de compartición de triaje.
 */
public interface TriageShareLinkJpaRepository extends JpaRepository<TriageShareLinkEntity, UUID> {

    Optional<TriageShareLinkEntity> findByToken(String token);

    Optional<TriageShareLinkEntity> findFirstByTriageIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID triageId);
}
