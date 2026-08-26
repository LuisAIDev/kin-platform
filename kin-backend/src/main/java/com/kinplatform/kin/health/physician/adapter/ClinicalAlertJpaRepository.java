package com.kinplatform.kin.health.physician.adapter;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de alertas clínicas (ADR-031).
 */
public interface ClinicalAlertJpaRepository extends JpaRepository<ClinicalAlertEntity, UUID> {

    List<ClinicalAlertEntity> findByPhysicianIdAndStatusOrderByCreatedAtDesc(
            UUID physicianId, com.kinplatform.kin.health.physician.domain.ClinicalAlert.AlertStatus status);
}
