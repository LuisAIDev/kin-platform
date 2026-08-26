package com.kinplatform.kin.health.triage.adapter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de condiciones (ADR-028).
 */
public interface ConditionJpaRepository extends JpaRepository<ConditionEntity, UUID> {

    Optional<ConditionEntity> findByName(String name);
}
