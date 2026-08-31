package com.kinplatform.kin.health.followup.adapter;

import com.kinplatform.kin.health.followup.domain.FollowUpStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio JPA de planes de seguimiento (ADR-033).
 */
public interface FollowUpPlanJpaRepository extends JpaRepository<FollowUpPlanEntity, UUID> {

    List<FollowUpPlanEntity> findByPatientIdAndPhysicianIdOrderByCreatedAtDesc(UUID patientId, UUID physicianId);

    List<FollowUpPlanEntity> findByPatientIdAndStatusOrderByCreatedAtDesc(UUID patientId, FollowUpStatus status);

    List<FollowUpPlanEntity> findByPhysicianIdOrderByCreatedAtDesc(UUID physicianId);

    @Query("select distinct p.physicianId from FollowUpPlanEntity p where p.status = :status")
    List<UUID> findDistinctPhysicianIdsByStatus(@Param("status") FollowUpStatus status);
}
