package com.kinplatform.kin.health.dashboard.adapter;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA del perfil del paciente (ADR-030).
 */
public interface PatientProfileJpaRepository extends JpaRepository<PatientProfileEntity, UUID> {}
