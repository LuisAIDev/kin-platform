package com.kinplatform.kin.health.differential.adapter;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de pruebas recomendadas (ADR-029).
 */
public interface RecommendedTestJpaRepository extends JpaRepository<RecommendedTestEntity, UUID> {}
