package com.kinplatform.kin.health.differential.adapter;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de factores de riesgo (ADR-029).
 */
public interface RiskFactorJpaRepository extends JpaRepository<RiskFactorEntity, UUID> {}
