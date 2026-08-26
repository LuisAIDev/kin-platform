package com.kinplatform.kin.health.triage.adapter;

import com.kinplatform.kin.health.triage.adapter.SymptomConditionRelationEntity.SymptomConditionRelationId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de relaciones síntoma ↔ condición (ADR-028).
 */
public interface SymptomConditionRelationJpaRepository
        extends JpaRepository<SymptomConditionRelationEntity, SymptomConditionRelationId> {}
