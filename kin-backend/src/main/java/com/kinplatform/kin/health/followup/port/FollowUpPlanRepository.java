package com.kinplatform.kin.health.followup.port;

import com.kinplatform.kin.health.followup.domain.FollowUpPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de planes de seguimiento (ADR-033).
 */
public interface FollowUpPlanRepository {

    FollowUpPlan save(FollowUpPlan plan);

    Optional<FollowUpPlan> findById(UUID id);

    /** Planes de un paciente creados por un médico (ordenados por creación desc). */
    List<FollowUpPlan> findByPatientIdAndPhysicianId(UUID patientId, UUID physicianId);

    /** Planes ACTIVOS de un paciente (vista del paciente). */
    List<FollowUpPlan> findActiveByPatientId(UUID patientId);

    /** Planes de todos los pacientes de un médico (para tareas vencidas y alertas). */
    List<FollowUpPlan> findByPhysicianId(UUID physicianId);

    /** Médicos con al menos un plan ACTIVO (para la alerta de evolución sin registrar). */
    List<UUID> findDistinctPhysicianIdsWithActivePlans();
}
