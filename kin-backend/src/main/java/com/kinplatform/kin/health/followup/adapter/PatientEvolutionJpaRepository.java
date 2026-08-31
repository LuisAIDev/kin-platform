package com.kinplatform.kin.health.followup.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de evolución del paciente (ADR-033).
 */
public interface PatientEvolutionJpaRepository extends JpaRepository<PatientEvolutionEntity, UUID> {

    List<PatientEvolutionEntity> findByPatientIdAndPhysicianIdOrderByRecordedAtDesc(UUID patientId, UUID physicianId);

    List<PatientEvolutionEntity> findByPatientIdOrderByRecordedAtDesc(UUID patientId);

    Optional<PatientEvolutionEntity> findTopByPatientIdOrderByRecordedAtDesc(UUID patientId);
}
