package com.kinplatform.kin.health.triage.adapter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de síntomas (ADR-028).
 */
public interface SymptomJpaRepository extends JpaRepository<SymptomEntity, UUID> {

    Optional<SymptomEntity> findByName(String name);
}
