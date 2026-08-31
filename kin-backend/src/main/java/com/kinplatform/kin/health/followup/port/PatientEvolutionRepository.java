package com.kinplatform.kin.health.followup.port;

import com.kinplatform.kin.health.followup.domain.PatientEvolution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de la evolución del paciente (ADR-033).
 */
public interface PatientEvolutionRepository {

    PatientEvolution save(PatientEvolution evolution);

    List<PatientEvolution> findByPatientIdAndPhysicianId(UUID patientId, UUID physicianId);

    /** Historial de evolución del paciente (vista del paciente, solo lectura). */
    List<PatientEvolution> findByPatientId(UUID patientId);

    /** Último registro de evolución del paciente (para la alerta de evolución sin registrar). */
    Optional<PatientEvolution> findLatestByPatientId(UUID patientId);
}
