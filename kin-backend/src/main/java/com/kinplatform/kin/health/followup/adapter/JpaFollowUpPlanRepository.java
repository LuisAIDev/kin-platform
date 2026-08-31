package com.kinplatform.kin.health.followup.adapter;

import com.kinplatform.kin.health.followup.domain.FollowUpPlan;
import com.kinplatform.kin.health.followup.domain.FollowUpStatus;
import com.kinplatform.kin.health.followup.port.FollowUpPlanRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link FollowUpPlanRepository} (ADR-033).
 */
@Component
public class JpaFollowUpPlanRepository implements FollowUpPlanRepository {

    private final FollowUpPlanJpaRepository repository;

    public JpaFollowUpPlanRepository(FollowUpPlanJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public FollowUpPlan save(FollowUpPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan no puede ser null");
        }
        FollowUpPlanEntity entity = repository.findById(plan.id()).orElseGet(FollowUpPlanEntity::new);
        entity.setId(plan.id());
        entity.setPhysicianId(plan.physicianId());
        entity.setPatientId(plan.patientId());
        entity.setTitle(plan.title());
        entity.setDescription(plan.description());
        entity.setStartDate(plan.startDate());
        entity.setEndDate(plan.endDate());
        entity.setFrequency(plan.frequency());
        entity.setStatus(plan.status());
        entity.setCreatedAt(plan.createdAt());
        repository.save(entity);
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FollowUpPlan> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(JpaFollowUpPlanRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpPlan> findByPatientIdAndPhysicianId(UUID patientId, UUID physicianId) {
        if (patientId == null || physicianId == null) {
            return List.of();
        }
        return repository
                .findByPatientIdAndPhysicianIdOrderByCreatedAtDesc(patientId, physicianId)
                .stream()
                .map(JpaFollowUpPlanRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpPlan> findActiveByPatientId(UUID patientId) {
        if (patientId == null) {
            return List.of();
        }
        return repository
                .findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, FollowUpStatus.ACTIVE)
                .stream()
                .map(JpaFollowUpPlanRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpPlan> findByPhysicianId(UUID physicianId) {
        if (physicianId == null) {
            return List.of();
        }
        return repository.findByPhysicianIdOrderByCreatedAtDesc(physicianId).stream()
                .map(JpaFollowUpPlanRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findDistinctPhysicianIdsWithActivePlans() {
        return repository.findDistinctPhysicianIdsByStatus(FollowUpStatus.ACTIVE);
    }

    private static FollowUpPlan toDomain(FollowUpPlanEntity e) {
        return FollowUpPlan.of(
                e.getId(),
                e.getPhysicianId(),
                e.getPatientId(),
                e.getTitle(),
                e.getDescription(),
                e.getStartDate(),
                e.getEndDate(),
                e.getFrequency(),
                e.getStatus(),
                e.getCreatedAt());
    }
}
