package com.kinplatform.kin.health.physician.port;

import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de alertas clínicas (ADR-031).
 *
 * <p>Guarda y consulta las alertas del portal de médicos. La infraestructura
 * lo implementa con JPA (tabla {@code clinical_alerts}).</p>
 */
public interface ClinicalAlertRepository {

    ClinicalAlert save(ClinicalAlert alert);

    Optional<ClinicalAlert> findByIdAndPhysician(UUID id, UUID physicianId);

    List<ClinicalAlert> findActiveByPhysician(UUID physicianId);
}
