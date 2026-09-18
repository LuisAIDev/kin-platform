package com.kinplatform.kin.health.triage.adapter;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de consultas de triaje (ADR-028).
 */
public interface TriageConsultationJpaRepository extends JpaRepository<TriageConsultationEntity, UUID> {

    List<TriageConsultationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<TriageConsultationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndCreatedAtBetween(UUID userId, java.time.OffsetDateTime start, java.time.OffsetDateTime end);

    Page<TriageConsultationEntity> findByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndHiddenAtIsNull(UUID userId);
}
