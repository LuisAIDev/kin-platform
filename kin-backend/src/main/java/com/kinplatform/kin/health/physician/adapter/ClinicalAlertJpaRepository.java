package com.kinplatform.kin.health.physician.adapter;

import com.kinplatform.kin.health.physician.domain.ClinicalAlert.AlertSeverity;
import com.kinplatform.kin.health.physician.domain.ClinicalAlert.AlertStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de alertas clínicas (ADR-031).
 */
public interface ClinicalAlertJpaRepository extends JpaRepository<ClinicalAlertEntity, UUID> {

    List<ClinicalAlertEntity> findByPhysicianIdAndStatusOrderByCreatedAtDesc(
            UUID physicianId, AlertStatus status);

    long countByPhysicianIdAndStatusAndSeverity(UUID physicianId, AlertStatus status, AlertSeverity severity);
}
